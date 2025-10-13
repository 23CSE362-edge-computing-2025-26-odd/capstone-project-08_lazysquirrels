import ray
from fastapi import FastAPI
from pydantic import BaseModel
from ray.rllib.agents.ppo import PPOTrainer
import numpy as np
import pickle

# Initialize Ray only once
if not ray.is_initialized():  # Check if Ray is already initialized
    ray.init(num_gpus=0, ignore_reinit_error=True)

# FastAPI app
app = FastAPI()

@app.get("/")
def read_root():
    return {"message": "Hello, World!"}

# Input observation format (from Edge Simulation)
class Obs(BaseModel):
    halting_counts: dict  # {"NS": <count>, "EW": <count>}
    speed_lag: dict       # {"NS": <speed>, "EW": <speed>}

class Query(BaseModel):
    sim_tick: int
    intersection_id: str
    min_phase_secs: int
    phase: str  # "NS_GREEN" or "EW_GREEN"
    phase_elapsed: float
    obs: Obs

# Define the environment similar to your simulation environment
class TrafficLightGridEnv:
    def __init__(self, config):
        self.config = config
        self.action_space = 2  # 2 actions: SWITCH or HOLD
        self.observation_space = np.random.uniform(low=0, high=10, size=(6,))  # Example observation space

    def reset(self):
        # Reset environment and return initial observation
        return self.observation_space

    def step(self, action):
        reward = -1
        if action == 1:  # If SWITCH is chosen
            reward = 10
        done = False
        return self.observation_space, reward, done, {}

# Load model from the params.pkl
def load_model_from_params():
    # Load the trained model parameters
    with open("ray_results/PPO_AccelEnv-v0_ca909474_2025-09-11_09-36-28e93kjeru/params.pkl", "rb") as f:
        model_params = pickle.load(f)

    # Initialize PPOTrainer (using TrafficLightGridEnv environment)
    trainer = PPOTrainer(env=TrafficLightGridEnv, config={})
    
    # Set the model weights manually (this is not the typical RLlib approach)
    trainer.get_policy().set_weights(model_params)
    
    return trainer

# Load the model
trainer = load_model_from_params()

# Action inference function
def get_action_from_model(obs):
    # Convert observation into a numpy array if needed (based on your model)
    action = trainer.compute_action(np.array(obs))
    return "SWITCH" if action == 1 else "HOLD"

# FastAPI POST endpoint for DRLE action
@app.post("/drle/act")
def drle_act(q: Query):
    # Convert the observation into the format the model can process
    obs = [
        q.obs.halting_counts.get("NS", 0), 
        q.obs.halting_counts.get("EW", 0), 
        10 - q.obs.speed_lag.get("NS", 0), 
        10 - q.obs.speed_lag.get("EW", 0),
        1 if q.phase == "NS_GREEN" else 0,  # Phase should be binary for model (NS_GREEN = 1, EW_GREEN = 0)
        q.phase_elapsed
    ]
    
    # Get action from the trained model
    action = get_action_from_model(obs)
    
    # Return the action
    return {"intersection_id": q.intersection_id, "action": action}
