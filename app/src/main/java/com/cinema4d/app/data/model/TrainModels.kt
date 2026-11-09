package com.cinema4d.app.data.model

data class Train(
    val id: String,
    val number: String,
    val name: String,
    val fromCode: String,
    val fromName: String,
    val toCode: String,
    val toName: String,
    val departure: String,
    val arrival: String,
    val arrivalDay: String,
    val durationLabel: String,
    val runsOn: String,
    val rating: Float,
    val classes: List<TrainClass>,
)

data class TrainClass(
    val id: String,
    val code: String,
    val label: String,
    val price: Int,
    val status: String,
)
