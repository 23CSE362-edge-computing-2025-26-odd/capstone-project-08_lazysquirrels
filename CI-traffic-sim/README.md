# Traffic Light DRLE Server

This repository contains the FastAPI-based DRLE (Deep Reinforcement Learning Edge) server for traffic light control. The server receives intersection observations from the edge simulation, processes them with a trained RL model (PPO/DQN), and returns the recommended action (HOLD or SWITCH) for each intersection.

---

## Prerequisites

1. **Python 3.9+**
2. **Install required packages** (without virtual environment):

   ```bash
   pip install requirements.txt
   ```

---

## Running the Server

1. **Start the DRLE server using Uvicorn**:

   ```bash
   uvicorn ci_server:app --reload --host 0.0.0.0 --port 8000
   ```

---

## DRLE API Usage

### POST `/drle/act`

Receives intersection state and returns the next action.

#### Request Body

```json
{
  "sim_tick": 0,
  "intersection_id": "i-0-0",
  "min_phase_secs": 3,
  "phase": "NS_GREEN",
  "phase_elapsed": 5.0,
  "obs": {
    "halting_counts": {"NS": 12, "EW": 8},
    "speed_lag": {"NS": 3, "EW": 2}
  }
}
```

#### Response

```json
{
  "intersection_id": "i-0-0",
  "action": "SWITCH"
}
```

* **Actions**: `"SWITCH"` or `"HOLD"`
