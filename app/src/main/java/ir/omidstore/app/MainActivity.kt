package ir.omidstore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Product(val id: Int, val title: String, val detail: String, val price: Int, val icon: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OmidStore() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OmidStore() {
    val products = remember {
        listOf(
            Product(1, "شیر کم‌چرب", "بطری یک لیتری", 32000, "🥛"),
            Product(2, "نان تازه", "بسته نان روز", 25000, "🍞"),
            Product(3, "تخم‌مرغ", "بسته ۶ عددی", 48000, "🥚"),
            Product(4, "سیب قرمز", "یک کیلوگرم", 65000, "🍎"),
            Product(5, "برنج ایرانی", "یک کیلوگرم", 185000, "🍚"),
            Product(6, "آب معدنی", "بطری یک و نیم لیتری", 12000, "💧")
        )
    }
    val cart = remember { mutableStateMapOf<Int, Int>() }
    var checkout by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val total = products.sumOf { it.price * (cart[it.id] ?: 0) }

    MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF147D55))) {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Column {
                        Text("فروشگاه امید", fontWeight = FontWeight.Bold)
                        Text("خرید آسان، تحویل در محل", style = MaterialTheme.typography.labelSmall)
                    }
                })
            },
            bottomBar = {
                if (cart.isNotEmpty()) {
                    Surface(shadowElevation = 8.dp) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("مبلغ سبد خرید")
                                Text(total.toString() + " تومان", fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = { checkout = true }) { Text("ثبت سفارش") }
                        }
                    }
                }
            }
        ) { insets ->
            LazyColumn(
                Modifier.fillMaxSize().padding(insets),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text("به فروشگاه امید خوش آمدید", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("محصولات را انتخاب کنید و هزینه را هنگام تحویل بپردازید.")
                        }
                    }
                    Text("محصولات", Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(products, key = { it.id }) { p ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(p.icon, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(end = 12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, fontWeight = FontWeight.Bold)
                                Text(p.detail, style = MaterialTheme.typography.bodySmall)
                                Text(p.price.toString() + " تومان", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            if ((cart[p.id] ?: 0) == 0) {
                                Button(onClick = { cart[p.id] = 1 }) { Text("افزودن") }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = {
                                        val n = cart[p.id] ?: 0
                                        if (n <= 1) cart.remove(p.id) else cart[p.id] = n - 1
                                    }) { Text("−") }
                                    Text((cart[p.id] ?: 0).toString())
                                    TextButton(onClick = { cart[p.id] = (cart[p.id] ?: 0) + 1 }) { Text("+") }
                                }
                            }
                        }
                    }
                }
                item {
                    Text("نسخه اولیه آزمایشی؛ اتصال سفارش واقعی و پنل مدیریت در مرحله بعد اضافه می‌شود.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
        if (checkout) {
            AlertDialog(
                onDismissRequest = { checkout = false },
                title = { Text("اطلاعات تحویل سفارش") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(name, { name = it }, label = { Text("نام و نام خانوادگی") })
                        OutlinedTextField(mobile, { mobile = it }, label = { Text("شماره موبایل") })
                        OutlinedTextField(address, { address = it }, label = { Text("نشانی کامل") }, minLines = 2)
                        Text("پرداخت در محل — مبلغ: " + total.toString() + " تومان")
                    }
                },
                confirmButton = {
                    Button(enabled = name.isNotBlank() && mobile.length >= 10 && address.isNotBlank(), onClick = {
                        notice = "فرم سفارش در نسخه آزمایشی تکمیل شد. برای دریافت واقعی سفارش باید سرور متصل شود."
                        cart.clear()
                        checkout = false
                    }) { Text("تأیید") }
                },
                dismissButton = { TextButton(onClick = { checkout = false }) { Text("انصراف") } }
            )
        }
        if (notice.isNotBlank()) {
            AlertDialog(onDismissRequest = { notice = "" }, title = { Text("فروشگاه امید") },
                text = { Text(notice) }, confirmButton = { TextButton(onClick = { notice = "" }) { Text("باشه") } })
        }
    }
}
