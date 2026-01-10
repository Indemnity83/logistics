# Asset Pipeline

## Pipe Models

Pipes are modeled using **Blockbench** and stored in `assets/models/`.

### Model Generation Workflow

1. **Author models in Blockbench**: Create pipe models with all components (core, arms, connections).
2. **Generate JSON models**: Run `scripts/generate_models.py` to split the Blockbench model files into individual component JSON files for Minecraft.
3. **Build blockstates by hand**: Blockstate files are manually authored to reassemble the right pieces based on pipe connections.

### Model Notes

- Prefer parented models with implicit UVs for pipe arms and extensions to avoid 1px shifts or mirrored faces on rotated blockstates.

## Pipe Textures

Textures are **generated dynamically** by `scripts/generate_pipe_textures.py`.

- Generation is deterministic—run it twice, get the same result
- Textures are code-generated for now; at some point an actual artist might paint them by hand for more style
