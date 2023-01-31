package com.example.msgshareapp

data class Hobbies(var title: String)

object Supplier {
    val hobbies = listOf<Hobbies>(
        Hobbies("Swimming"),
        Hobbies("Reading"),
        Hobbies("Eating"),
        Hobbies("Singing"),
        Hobbies("Outing"),
        Hobbies("Paragliding"),
        Hobbies("Drinking"),
        Hobbies("Catering"),
        Hobbies("Joking"),
        Hobbies("Bathing"),
        Hobbies("Dancing")
    )
}