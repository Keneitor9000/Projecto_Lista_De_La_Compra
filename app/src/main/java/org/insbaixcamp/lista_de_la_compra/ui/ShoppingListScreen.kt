package org.insbaixcamp.lista_de_la_compra.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.insbaixcamp.lista_de_la_compra.model.Category
import org.insbaixcamp.lista_de_la_compra.model.Product
import org.insbaixcamp.lista_de_la_compra.model.ShoppingList
import org.insbaixcamp.lista_de_la_compra.ui.theme.Lista_De_La_CompraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingAppScreen(
    currentUserEmail: String = "usuario@ejemplo.com",
    onSignOut: () -> Unit = {}
) {
    // Listas sin datos de ejemplo (inician vacías)
    val lists = remember { mutableStateListOf<ShoppingList>() }
    val categories = remember { mutableStateListOf<Category>() }
    val products = remember { mutableStateListOf<Product>() }

    var selectedList by remember { mutableStateOf<ShoppingList?>(null) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var listToDeleteConfirm by remember { mutableStateOf<ShoppingList?>(null) }

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
                        items(lists) { list ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedList = list }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                                        val isOwner = list.ownerEmail == currentUserEmail
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

        // Diálogo de confirmación para eliminar lista
        listToDeleteConfirm?.let { list ->
            AlertDialog(
                onDismissRequest = { listToDeleteConfirm = null },
                title = { Text("¿Eliminar lista?") },
                text = { Text("¿Estás seguro de que quieres eliminar la lista \"${list.name}\"?") },
                confirmButton = {
                    Button(onClick = {
                        lists.remove(list)
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
                            lists.add(ShoppingList(name = newListName, ownerEmail = currentUserEmail))
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
        val currentList = selectedList!!
        ShoppingListDetailScreen(
            shoppingList = currentList,
            categories = categories.filter { it.listId == currentList.id },
            products = products,
            onBack = { selectedList = null },
            onAddCategory = { name -> categories.add(Category(listId = currentList.id, name = name)) },
            onEditCategory = { category, newName ->
                val index = categories.indexOfFirst { it.id == category.id }
                if (index != -1) categories[index] = category.copy(name = newName)
            },
            onDeleteCategory = { category ->
                categories.remove(category)
                products.removeAll { it.categoryId == category.id }
            },
            onAddProduct = { categoryId, name -> products.add(Product(categoryId = categoryId, name = name)) },
            onEditProduct = { product, newName ->
                val index = products.indexOfFirst { it.id == product.id }
                if (index != -1) products[index] = product.copy(name = newName)
            },
            onToggleProduct = { product ->
                val index = products.indexOfFirst { it.id == product.id }
                if (index != -1) products[index] = product.copy(isCompleted = !product.isCompleted)
            },
            onDeleteProduct = { product -> products.remove(product) },
            onShareList = { email ->
                val index = lists.indexOfFirst { it.id == currentList.id }
                if (index != -1) {
                    val updated = lists[index].copy(sharedWithEmails = lists[index].sharedWithEmails + email)
                    lists[index] = updated
                    selectedList = updated
                }
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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(categories) { category ->
                        val categoryProducts = products.filter { it.categoryId == category.id }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = category.name, style = MaterialTheme.typography.titleLarge)
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
