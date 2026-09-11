I already have a working robot-control application. Do NOT redesign or remove my existing features.

CURRENT SYSTEM
The robot consists of:
- ESP32 Motor Controller
- L298N motor driver
- DC motors
- DHT11 temperature/humidity sensor
- HC-SR04 ultrasonic distance sensor
- MQ gas sensor
- HW-038 water sensor
- ESP32-CAM
- Servo motor

My application already provides:
1. Motor control
2. ESP32-CAM live video
3. ESP32-CAM servo control
4. Live sensor readings
5. Sensor data graphs

The ESP32 provides sensor data through its existing API.

Motor ESP32:
http://172.17.40.50

Sensor endpoint:
http://172.17.40.50/data

The /data endpoint provides:
{
  "distance": number,
  "temperature": number,
  "humidity": number,
  "mq": number,
  "water": number
}

IMPORTANT GOAL
I want to add ONLY a SIMPLE AI/ML FUTURE-PREDICTION FEATURE.

I do NOT want:
- Deep learning
- Neural networks
- LSTM
- TensorFlow
- Large AI models
- Cloud AI APIs
- ChatGPT API
- Complicated backend
- Heavy processing
- Any feature that can interfere with motor control or camera streaming

The AI should be lightweight and should predict the next sensor value using recent historical sensor readings.

DATA STORAGE
Do NOT continuously store an unlimited amount of sensor data in application memory.

The application should maintain only a small rolling window of recent readings for prediction, for example 20–50 readings per sensor.

Example:

MQ:
420
425
431
440
448

When a new reading arrives, the oldest reading should be removed.

Example:
Before:
[420, 425, 431, 440, 448]

New:
456

After:
[425, 431, 440, 448, 456]

This keeps memory usage constant.

HISTORICAL DATA STORAGE
If permanent historical data is required, store it separately from the application's live-memory prediction buffer.

Use a simple structured format such as CSV or JSON.

Example CSV:

timestamp,temperature,humidity,distance,mq,water
2026-09-11T18:00:01,28.4,61,45,420,120
2026-09-11T18:00:02,28.4,61,44,425,121
2026-09-11T18:00:03,28.5,60,44,431,121

The permanent historical storage must NOT be loaded completely into RAM.

Only the small recent window should be loaded when required for prediction.

DATA CLEANING
Before using readings for prediction, perform lightweight data cleaning.

1. Ignore missing/null readings.
2. Ignore invalid sensor values.
3. Check whether a reading is numeric.
4. Remove obvious abnormal/outlier values using a simple method.
5. Make sure timestamps are valid and ordered.
6. Do not modify the original raw historical data.
7. Keep raw data separate from cleaned/prediction data.

Use this logical structure:

RAW DATA
    ↓
Validation
    ↓
Cleaning
    ↓
Recent valid readings
    ↓
AI prediction

Do NOT aggressively filter real sensor changes. The purpose is only to remove clearly invalid readings.

AI / ML PREDICTION
Use a lightweight prediction algorithm.

Preferred approach:
- Simple Linear Regression

The model should use recent valid readings to estimate the next reading.

Example:

Recent MQ readings:
420 → 425 → 431 → 440 → 448

The model learns the recent trend and predicts approximately:

Next MQ ≈ 456

The prediction must be clearly labelled as a prediction/estimate, not as actual sensor data.

PREDICTION FREQUENCY
Do not run prediction unnecessarily on every UI operation.

The application should:
1. Receive live sensor data.
2. Update the live display.
3. Add the new valid reading to the rolling prediction buffer.
4. Remove the oldest reading when the buffer exceeds its maximum size.
5. Run the lightweight prediction periodically or when enough new readings are available.
6. Update the prediction display.

LIVE DATA MUST ALWAYS REMAIN INDEPENDENT
The live sensor data must continue to come directly from the ESP32.

Architecture:

ESP32
   ↓
Live sensor data
   ↓
Application
   ├── Live display
   ├── Graph
   └── Recent-data buffer
            ↓
       Lightweight ML
            ↓
       Future prediction

The AI must NEVER replace the live sensor reading.

UI REQUIREMENTS

For every supported sensor, display:

LIVE VALUE
PREDICTED NEXT VALUE
TREND

Example:

MQ GAS

LIVE
448

AI PREDICTION
456

TREND
↑ Increasing

The live value must be visually distinguishable from the prediction.

GRAPH REQUIREMENT

I already have live sensor graphs.

Do not remove the existing graph.

Add the prediction to the same graph.

Use:

Solid line = actual/live sensor data
Dotted/dashed line = AI predicted data

Clearly mark the current time as:

NOW

The graph should look conceptually like:

Past                     Future
──────────────────│────────────────
       ●──●──●──●│ - - - - - ●
     ●            │       - - ●
                  │
                 NOW

The historical/live line represents actual sensor measurements.

The dotted line represents future predicted values.

Do NOT present predicted values as if they were measured values.

PREDICTION HORIZON
Keep the prediction simple.

Initially predict only:
- Next reading

Optionally allow:
- Next 30 seconds
- Next 1 minute

Do not implement long-term forecasting.

SENSORS
Initially support prediction for:
1. Temperature
2. MQ gas sensor
3. Water sensor

Distance and humidity may be added if the prediction is useful, but do not unnecessarily add complexity.

PERFORMANCE REQUIREMENTS
The robot controls have higher priority than AI.

Motor commands must remain responsive.

Camera streaming must remain responsive.

AI processing must:
- Use very little CPU
- Use very little memory
- Run independently from motor-control code
- Never block the main UI
- Never block camera streaming
- Fail safely if insufficient data exists

If there are fewer than the minimum required valid readings, display:

"Collecting data..."

instead of generating a fake prediction.

ERROR HANDLING

If AI prediction fails:

Live sensor data must continue working.

Display:

AI Prediction: Unavailable

Do not stop:
- Motor control
- Camera
- Servo
- Live sensor readings
- Existing graphs

DATA PRIVACY / NETWORK

Do not send sensor data to external AI services.

The prediction should run locally.

If permanent online storage is implemented, clearly separate it from the local prediction process.

Do not use GitHub as a real-time database.

If GitHub is used, use it only for occasional historical-data backup/export, not for every sensor reading.

FINAL ARCHITECTURE

                    ROBOT
                      │
                      ▼
                  ESP32
                      │
                      │ Wi-Fi
                      ▼
                ┌───────────┐
                │    APP    │
                └─────┬─────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
   Live Sensors    Graphs       Robot Control
        │
        ▼
 Small Rolling Buffer
   20–50 readings
        │
        ▼
 Data Validation
        │
        ▼
 Data Cleaning
        │
        ▼
 Lightweight ML
 (Linear Regression)
        │
        ▼
 Next-value Prediction
        │
        ▼
 Live + Prediction Graph

IMPORTANT:
Preserve all existing application functionality.
Do not rewrite the entire application unless necessary.
Add the AI prediction as an independent module/component.
Keep the implementation simple, lightweight, and suitable for an academic robot project.
