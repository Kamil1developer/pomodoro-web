import base64
import io
import os
from contextlib import asynccontextmanager
from dataclasses import dataclass
from typing import Optional

import torch
from diffusers import AutoPipelineForText2Image
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field


MODEL_ID = os.getenv("LOCAL_IMAGE_MODEL_ID", "hf-internal-testing/tiny-stable-diffusion-pipe")
DEVICE = "cpu"
DTYPE = torch.float32


@dataclass
class RuntimeState:
    pipeline: Optional[AutoPipelineForText2Image] = None
    model_id: str = MODEL_ID


state = RuntimeState()


def load_pipeline(model_id: str) -> AutoPipelineForText2Image:
    errors: list[str] = []
    strategies = [
        {"use_safetensors": True},
        {"use_safetensors": False},
        {},
    ]
    for strategy in strategies:
        try:
            return AutoPipelineForText2Image.from_pretrained(
                model_id,
                torch_dtype=DTYPE,
                **strategy,
            )
        except Exception as exc:
            errors.append(f"{strategy}: {exc}")
    raise RuntimeError("; ".join(errors))


class GenerateRequest(BaseModel):
    prompt: str = Field(min_length=3, max_length=1024)
    negative_prompt: str = Field(default="blurry, low quality, artifacts", max_length=1024)
    width: int = Field(default=512, ge=256, le=1024)
    height: int = Field(default=512, ge=256, le=1024)
    num_inference_steps: int = Field(default=6, ge=1, le=30)
    guidance_scale: float = Field(default=0.0, ge=0.0, le=15.0)
    seed: Optional[int] = None


class GenerateResponse(BaseModel):
    b64_image: str
    model: str
    used_prompt: str


@asynccontextmanager
async def lifespan(_: FastAPI):
    try:
        pipeline = load_pipeline(MODEL_ID)
        # Tiny test pipelines can have an incompatible safety checker config.
        if hasattr(pipeline, "safety_checker"):
            pipeline.safety_checker = None
        if hasattr(pipeline, "register_to_config"):
            pipeline.register_to_config(requires_safety_checker=False)
        pipeline = pipeline.to(DEVICE)
        pipeline.set_progress_bar_config(disable=True)
        pipeline.enable_attention_slicing()
        state.pipeline = pipeline
    except Exception as exc:
        state.pipeline = None
        print(f"Failed to initialize model '{MODEL_ID}': {exc}")
    yield


app = FastAPI(title="Local Image Service", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health():
    return {
        "ok": state.pipeline is not None,
        "model": state.model_id,
        "device": DEVICE,
    }


@app.post("/generate", response_model=GenerateResponse)
def generate(payload: GenerateRequest):
    if state.pipeline is None:
        raise HTTPException(
            status_code=503,
            detail="Image model is not initialized. Check service logs.",
        )

    generator = None
    if payload.seed is not None:
        generator = torch.Generator(device=DEVICE).manual_seed(payload.seed)

    image = state.pipeline(
        prompt=payload.prompt,
        negative_prompt=payload.negative_prompt,
        width=payload.width,
        height=payload.height,
        guidance_scale=payload.guidance_scale,
        num_inference_steps=payload.num_inference_steps,
        generator=generator,
    ).images[0]

    buffer = io.BytesIO()
    image.save(buffer, format="PNG")

    return GenerateResponse(
        b64_image=base64.b64encode(buffer.getvalue()).decode("utf-8"),
        model=state.model_id,
        used_prompt=payload.prompt,
    )
