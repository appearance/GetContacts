package com.syed.getcontacts

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.syed.getcontacts.ui.theme.GetContactsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GetContactsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenAndSelectContact()
                }
            }
        }
    }
}

@Composable
@Preview(showBackground=true)
fun OpenAndSelectContact() {
    val context = LocalContext.current
    var contactName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    // Create a intent variable
    val contactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI).apply {
        type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
    }
    contactIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

    val launchContactForResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data

            /*val projection: Array<String> = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )*/

            contactUri?.let { uri ->

                // Get contact-ID and name
                val projection: Array<String> = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME
                )
                var id = -1L
                context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
                    if (cursor!!.moveToFirst()) {
                        id = cursor.getLong(0)
                        val name = cursor.getString(1)
                        Log.d("syed", "id: $id, name: $name")
                    }
                }

                // Get e-mail and phone using the contact-ID
                val projection2: Array<String> = arrayOf(
                    Data.MIMETYPE, Data.DATA1
                )
                val selection2 = Data.CONTACT_ID + "=" + id + " AND " + Data.MIMETYPE + " IN ('" + ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE + "', '" + Email.CONTENT_ITEM_TYPE + "')"
                context.contentResolver.query(Data.CONTENT_URI, projection2, selection2, null, null).use { cursor ->
                    while(cursor!!.moveToNext()) {
                        val mimetype = cursor.getString(0)
                        val data = cursor.getString(1)
                        if (mimetype == Phone.CONTENT_ITEM_TYPE) {
                            Log.d("syed", "Got phone: $data")
                        } else {
                            Log.d("syed", "Got email: $data")
                        }
                    }
                }

                /*context.contentResolver.query(it, projection, null, null, null).use { cursor ->
                    // If the cursor returned is validl get the phone number and (or) name
                    if (cursor!!.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val number = cursor.getString(numberIndex)

                        val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val name = cursor.getString(nameIndex)
                        contactName = name
                        contactNumber = number

                        // Do somethingwith the phone number
                        Toast.makeText(
                            context, "Number is $number & name is $name", Toast.LENGTH_SHORT
                        ).show()
                    }
                }*/
            }
        }
    }

    val launchContactPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            launchContactForResult.launch(contactIntent)
        } else {
            Toast.makeText(context, "Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            content = { Text("IMPORT FROM CONTACT") },
            onClick = {
                when(PackageManager.PERMISSION_GRANTED) {
                    // First time asking for permission ... to be granted by user
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_CONTACTS
                    ) -> {
                        launchContactForResult.launch(contactIntent)
                    }

                    else -> {
                        // If permission has been already granted
                        launchContactPermission.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Selected Contact Name: $contactName")
        Text(text = "Selected Contact Number: $contactNumber")
    }
}
