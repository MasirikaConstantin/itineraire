package com.mascode.itineraire.domain.model

enum class PlaceCategory {
    HOME,
    UNIVERSITY,
    WORK,
    CHURCH,
    TRANSPORT_STOP,
    OTHER,
}

enum class DayEventType {
    WAKE_UP,
    LEAVE_HOME,
    ARRIVAL,
    ACTIVITY,
    END_OF_DAY,
}

enum class JourneyStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class TransportMode {
    WALK,
    TAXI,
    TAXI_BUS,
    BUS,
    MOTORCYCLE,
    BICYCLE,
    PERSONAL_CAR,
    OTHER,
}

enum class ObservationType {
    TRAFFIC,
    WAITING,
    BREAKDOWN,
    WEATHER,
    OTHER,
}
