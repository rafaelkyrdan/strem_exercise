# Stream processing exercise
## Getting started
1. This project includes sbt file with necessary libs ready to go (akka, fs2, testing, mock libs)
2. Feel free to add other libs as required

## Requirements

Write a program that consumes 2 source streams

1. One which contains charging data

```
{
    "timestamp": "2023-11-02T17:08:49Z",
    "socketId": "s123",
    "vehicleId": "v123",
    "powerInWatts": 60
}
```

2. One which contains battery data

```
{
    "timestamp": "2023-11-02T17:08:49Z",
    "vehicleId": "v123"
    "stateOfChargeInPercent": 51
}
```

and combines them to produce the following output

```
{
    "timestamp": "2023-11-02T17:08:50Z",
    "socketId": "s123",
    "vehicleId": "v123",
    "powerInWatts": 60,
    "stateOfChargeInPercent": 51
}
```

### Notes
1. Only emit output when charging data arrives
2. Data can be for different socketId and vehicleId
3. Output contains the last received stateOfChargeInPercent

### Implementation notes
1. Please feel free to use akka streams or fs2 library
2. Assume inputs and outputs as case classes (no need to use codecs)# strem_exercise
