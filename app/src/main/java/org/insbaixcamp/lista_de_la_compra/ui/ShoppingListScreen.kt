package org.insbaixcamp.lista_de_la_compra.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import org.insbaixcamp.lista_de_la_compra.model.Category
import org.insbaixcamp.lista_de_la_compra.model.Product
import org.insbaixcamp.lista_de_la_compra.model.ShoppingList
import org.insbaixcamp.lista_de_la_compra.ui.theme.Lista_De_La_CompraTheme

private const val TAG = "ShoppingAppScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingAppScreen(
    currentUserEmail: String = "usuario@ejemplo.com",
    onSignOut: () -> Unit = {}
) {
    val isPreview = LocalInspectionMode.current
    val db = remember { if (!isPreview) Firebase.firestore else null }

    var lists by remember { mutableStateOf<List<ShoppingList>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }

    var selectedListId by remember { mutableStateOf<String?>(null) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var listToDeleteConfirm by remember { mutableStateOf<ShoppingList?>(null) }

    val selectedList = lists.find { it.id == selectedListId }

    // --- Sincronización en tiempo real con Firebase Firestore ---

    // 1. Escuchar la colección de LISTAS
    DisposableEffect(currentUserEmail, db) {
        if (db == null) return@DisposableEffect onDispose {}
        val listener = db.collection("lists")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando listas de Firestore", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fetched = snapshot.documents.mapNotNull { it.toObject(ShoppingList::class.java) }
                        .filter { item ->
                            item.ownerEmail.equals(currentUserEmail, ignoreCase = true) ||
                                    item.sharedWithEmails.any { it.equals(currentUserEmail, ignoreCase = true) } ||
                                    currentUserEmail.contains("demo")
                        }
                    lists = fetched
                }
            }
        onDispose { listener.remove() }
    }

    // 2. Escuchar la colección de CATEGORÍAS
    DisposableEffect(db) {
        if (db == null) return@DisposableEffect onDispose {}
        val listener = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando categorías", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    categories = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
                }
            }
        onDispose { listener.remove() }
    }

    // 3. Escuchar la colección de PRODUCTOS
    DisposableEffect(db) {
        if (db == null) return@DisposableEffect onDispose {}
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando productos", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    products = snapshot.documents.mapNotNull { it.toObject(Product::class.java) }
                }
            }
        onDispose { listener.remove() }
    }

    if (selectedList == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Mis Listas de la Compra") },
                    actions = { TextButton(onClick = onSignOut) { Text("Salir") } }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
            ) {
                Button(
                    onClick = { showCreateListDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Nueva Lista")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (lists.isEmpty()) {
                    Text(
                        text = "No tienes listas creadas.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lists, key = { it.id }) { list ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedListId = list.id }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                                        val isOwner = list.ownerEmail.equals(currentUserEmail, ignoreCase = true)
                                        Text(
                                            text = if (isOwner) "Propia" else "Compartida por ${list.ownerEmail}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = { listToDeleteConfirm = list }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar lista")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de confirmación para eliminar lista (Borrado en Cascada)
        listToDeleteConfirm?.let { list ->
            AlertDialog(
                onDismissRequest = { listToDeleteConfirm = null },
                title = { Text("¿Eliminar lista?") },
                text = { Text("¿Estás seguro de que quieres eliminar la lista \"${list.name}\"? Se borrarán también sus categorías y productos.") },
                confirmButton = {
                    Button(onClick = {
                        // Encontrar las categorías y productos pertenecientes a esta lista
                        val targetCategories = categories.filter { it.listId == list.id }
                        val targetCategoryIds = targetCategories.map { it.id }.toSet()
                        val targetProducts = products.filter { it.categoryId in targetCategoryIds }

                        // Actualización local inmediata
                        lists = lists.filterNot { it.id == list.id }
                        categories = categories.filterNot { it.listId == list.id }
                        products = products.filterNot { it.categoryId in targetCategoryIds }

                        // Eliminar en Firestore (Lista, Categorías y Productos)
                        db?.collection("lists")?.document(list.id)?.delete()
                        for (category in targetCategories) {
                            db?.collection("categories")?.document(category.id)?.delete()
                        }
                        for (product in targetProducts) {
                            db?.collection("products")?.document(product.id)?.delete()
                        }

                        listToDeleteConfirm = null
                    }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { listToDeleteConfirm = null }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo para crear lista
        if (showCreateListDialog) {
            var newListName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreateListDialog = false },
                title = { Text("Nueva Lista") },
                text = {
                    OutlinedTextField(
                        value = newListName,
                        onValueChange = { newListName = it },
                        label = { Text("Nombre de la lista") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newListName.isNotBlank()) {
                            val newList = ShoppingList(name = newListName, ownerEmail = currentUserEmail)
                            lists = lists + newList
                            db?.collection("lists")?.document(newList.id)?.set(newList)
                            showCreateListDialog = false
                        }
                    }) { Text("Crear") }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateListDialog = false }) { Text("Cancelar") }
                }
            )
        }
    } else {
        val currentList = selectedList
        ShoppingListDetailScreen(
            shoppingList = currentList,
            categories = categories.filter { it.listId == currentList.id },
            products = products,
            onBack = { selectedListId = null },
            onAddCategory = { name ->
                val newCat = Category(listId = currentList.id, name = name)
                categories = categories + newCat
                db?.collection("categories")?.document(newCat.id)?.set(newCat)
            },
            onEditCategory = { category, newName ->
                categories = categories.map { if (it.id == category.id) it.copy(name = newName) else it }
                db?.collection("categories")?.document(category.id)?.update("name", newName)
            },
            onDeleteCategory = { category ->
                val targetProducts = products.filter { it.categoryId == category.id }

                categories = categories.filterNot { it.id == category.id }
                products = products.filterNot { it.categoryId == category.id }

                db?.collection("categories")?.document(category.id)?.delete()
                for (product in targetProducts) {
                    db?.collection("products")?.document(product.id)?.delete()
                }
            },
            onAddProduct = { categoryId, name ->
                val newProduct = Product(categoryId = categoryId, name = name)
                products = products + newProduct
                db?.collection("products")?.document(newProduct.id)?.set(newProduct)
            },
            onEditProduct = { product, newName ->
                products = products.map { if (it.id == product.id) it.copy(name = newName) else it }
                db?.collection("products")?.document(product.id)?.update("name", newName)
            },
            onToggleProduct = { product ->
                val updatedProduct = product.copy(isCompleted = !product.isCompleted)
                products = products.map { if (it.id == product.id) updatedProduct else it }
                db?.collection("products")?.document(product.id)?.update("isCompleted", updatedProduct.isCompleted)
            },
            onDeleteProduct = { product ->
                products = products.filterNot { it.id == product.id }
                db?.collection("products")?.document(product.id)?.delete()
            },
            onShareList = { email ->
                val updatedList = currentList.copy(sharedWithEmails = currentList.sharedWithEmails + email)
                lists = lists.map { if (it.id == currentList.id) updatedList else it }
                db?.collection("lists")?.document(currentList.id)
                    ?.update("sharedWithEmails", FieldValue.arrayUnion(email))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListDetailScreen(
    shoppingList: ShoppingList,
    categories: List<Category>,
    products: List<Product>,
    onBack: () -> Unit,
    onAddCategory: (String) -> Unit,
    onEditCategory: (Category, String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onAddProduct: (String, String) -> Unit,
    onEditProduct: (Product, String) -> Unit,
    onToggleProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onShareList: (String) -> Unit
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var categoryToDeleteConfirm by remember { mutableStateOf<Category?>(null) }
    var categoryForNewProduct by remember { mutableStateOf<Category?>(null) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDeleteConfirm by remember { mutableStateOf<Product?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    var selectedCategoryFilterId by remember { mutableStateOf<String?>(null) }
    val collapsedCategoryIds = remember { mutableStateListOf<String>() }

    val displayedCategories = if (selectedCategoryFilterId == null) {
        categories
    } else {
        categories.filter { it.id == selectedCategoryFilterId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shoppingList.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir lista")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Button(
                onClick = { showCategoryDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Añadir Categoría")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                Text(
                    text = "Añade una categoría para empezar.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = "Filtrar por categoría:",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = (selectedCategoryFilterId == null),
                        onClick = { selectedCategoryFilterId = null },
                        label = { Text("Ver todas") }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = (selectedCategoryFilterId == category.id),
                            onClick = { selectedCategoryFilterId = category.id },
                            label = { Text(category.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(displayedCategories, key = { it.id }) { category ->
                        val categoryProducts = products.filter { it.categoryId == category.id }
                        val isCollapsed = collapsedCategoryIds.contains(category.id)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (isCollapsed) collapsedCategoryIds.remove(category.id)
                                                else collapsedCategoryIds.add(category.id)
                                            }
                                    ) {
                                        IconButton(onClick = {
                                            if (isCollapsed) collapsedCategoryIds.remove(category.id)
                                            else collapsedCategoryIds.add(category.id)
                                        }) {
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                contentDescription = if (isCollapsed) "Desplegar" else "Plegar"
                                            )
                                        }
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }

                                    Row {
                                        IconButton(onClick = { categoryForNewProduct = category }) {
                                            Icon(Icons.Default.Add, contentDescription = "Añadir producto")
                                        }
                                        IconButton(onClick = { categoryToEdit = category }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar categoría")
                                        }
                                        IconButton(onClick = { categoryToDeleteConfirm = category }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Eliminar categoría")
                                        }
                                    }
                                }

                                if (!isCollapsed) {
                                    categoryProducts.forEach { product ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = product.isCompleted,
                                                onCheckedChange = { onToggleProduct(product) }
                                            )
                                            Text(
                                                text = product.name,
                                                modifier = Modifier.weight(1f),
                                                textDecoration = if (product.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            IconButton(onClick = { productToEdit = product }) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editar producto")
                                            }
                                            IconButton(onClick = { productToDeleteConfirm = product }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar producto")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo confirmación eliminar categoría
    categoryToDeleteConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDeleteConfirm = null },
            title = { Text("¿Eliminar categoría?") },
            text = { Text("¿Estás seguro de que quieres eliminar la categoría \"${category.name}\"?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteCategory(category)
                    categoryToDeleteConfirm = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo confirmación eliminar producto
    productToDeleteConfirm?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDeleteConfirm = null },
            title = { Text("¿Eliminar producto?") },
            text = { Text("¿Estás seguro de que quieres eliminar \"${product.name}\"?") },
            confirmButton = {
                Button(onClick = {
                    onDeleteProduct(product)
                    productToDeleteConfirm = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { productToDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    if (showCategoryDialog) {
        var categoryName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Nueva Categoría") },
            text = {
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nombre de la categoría") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (categoryName.isNotBlank()) {
                        onAddCategory(categoryName)
                        showCategoryDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Cancelar") }
            }
        )
    }

    categoryToEdit?.let { category ->
        var newName by remember { mutableStateOf(category.name) }
        AlertDialog(
            onDismissRequest = { categoryToEdit = null },
            title = { Text("Editar Categoría") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre de categoría") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onEditCategory(category, newName)
                        categoryToEdit = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToEdit = null }) { Text("Cancelar") }
            }
        )
    }

    categoryForNewProduct?.let { category ->
        var productName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { categoryForNewProduct = null },
            title = { Text("Nuevo Producto en ${category.name}") },
            text = {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Nombre del producto") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (productName.isNotBlank()) {
                        onAddProduct(category.id, productName)
                        categoryForNewProduct = null
                    }
                }) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { categoryForNewProduct = null }) { Text("Cancelar") }
            }
        )
    }

    productToEdit?.let { product ->
        var newName by remember { mutableStateOf(product.name) }
        AlertDialog(
            onDismissRequest = { productToEdit = null },
            title = { Text("Editar Producto") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre del producto") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        onEditProduct(product, newName)
                        productToEdit = null
                    }
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { productToEdit = null }) { Text("Cancelar") }
            }
        )
    }

    if (showShareDialog) {
        var emailToShare by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Compartir Lista") },
            text = {
                Column {
                    Text("Escribe el email del usuario:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailToShare,
                        onValueChange = { emailToShare = it },
                        label = { Text("Email del usuario") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (emailToShare.isNotBlank()) {
                        onShareList(emailToShare)
                        showShareDialog = false
                    }
                }) { Text("Compartir") }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ShoppingAppScreenPreview() {
    Lista_De_La_CompraTheme {
        ShoppingAppScreen()
    }
}
