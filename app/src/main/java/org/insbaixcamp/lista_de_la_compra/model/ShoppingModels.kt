package org.insbaixcamp.lista_de_la_compra.model

import java.util.UUID

/**
 * Modelo para la lista de la compra.
 * Es única y personal, pero incluye `sharedWithEmails` para compartirla con otros usuarios.
 */
data class ShoppingList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ownerEmail: String,
    val sharedWithEmails: List<String> = emptyList()
)

/**
 * Categoría dentro de una lista (ej: "Lácteos", "Frutas").
 */
data class Category(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String
)

/**
 * Producto perteneciente a una categoría.
 */
data class Product(
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val isCompleted: Boolean = false
)
