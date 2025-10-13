import gym
from gym import spaces
import numpy as np

class TrafficLightGridEnv(gym.Env):
    """
    Custom Environment for the Traffic Light Grid Simulation
    """
    def __init__(self, config):
        super(TrafficLightGridEnv, self).__init__()
        self.grid_size = config["grid_size"]  # Grid dimensions (NxN)
        self.action_space = spaces.Discrete(2)  # 2 actions: 0 = HOLD, 1 = SWITCH
        self.observation_space = spaces.Box(low=0, high=10, shape=(6,), dtype=np.float32)  # Example observation (queue lengths, speeds)

    def reset(self):
        # Reset the environment state (e.g., traffic queues, speeds)
        self.state = np.random.uniform(low=0, high=10, size=(6,))  # Dummy state, adjust as needed
        return self.state

    def step(self, action):
        """
        Apply action (SWITCH or HOLD) and calculate the next state and reward.
        """
        reward = -1  # Default penalty for taking an action
        if action == 1:  # SWITCH
            reward = 10  # Reward for switching lights (for example)

        done = False  # Define if the episode is finished, here we're not setting an end condition

        # Update the state (simulate traffic update)
        self.state = np.random.uniform(low=0, high=10, size=(6,))  # Dummy state update, adjust based on logic
        return self.state, reward, done, {}

    def render(self):
        # Optional: Render the environment for visualization (e.g., print the state)
        print("Current state:", self.state)
