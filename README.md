# Neuralblock
A software suite for creating AI generated minecraft worlds. The Neuralblock project consists of these components:

- A Minecraft mod that can:
    - Create a block grid dataset from the player's POV in a regular minecraft world
    - Generate blocks using an AI model in a Neuralblock-generated world (🧪**Work in progress**)
- Dataset processing scripts (To prepare data for training)
- Model training scripts (that produces a model for the Minecraft mod)

## Play with an existing model
World generation is not yet available.

## Creating your own model
(Note: This process may change as the mod develops futher)

#### Create the dataset
1. Download `neuralblock-x.y.z.jar` from the [releases page](https://github.com/TheIcyStar/NeuralBlock/releases/)
2. Install the mod with **Minecraft 1.21.4** and **NeoForge** \
(You can use a launcher like [PrismLauncher]() to do this)
3. Enter any minecraft world and the mod will begin collecting block data
4. Exit the game
5. Your dataset will be in `$USERHOME/Downloads/NeuralBlockCSVs`

#### Dataset processing
1. Navigate to the [preprocessor folder](nnModel/preprocessor)
2. Follow its README.md to combine multiple CSVs into one and to "downcast" all of the block types

#### Model training
1. Navigate to the [training folder](nnModel/training)
2. Use `NeuralNetPredictor.ipynb` to create the AI model. (For convinience, you can open the Jupyter Notebook in Google Colab)

## Acknowledgements
*Inspired by [DecartAI's Oasis](https://www.decart.ai/articles/oasis-interactive-ai-video-game-model), a video-based open-world AI model*.