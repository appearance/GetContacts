package com.syed.getcontacts

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
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
    var logData by remember { mutableStateOf("") }

    // Create a intent variable
    val contactIntent = Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI)
    contactIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

    val launchContactForResult = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.data

            if (uri != null) {
                val cr = context.contentResolver
                val cur = cr.query(uri, null, null, null, null)
                cur?.moveToFirst()

                val id = cur?.getColumnIndex(Contacts._ID)?.let { cur.getString(it) }
                val name = cur?.getColumnIndex(Contacts.DISPLAY_NAME)?.let { cur.getString(it) }
                logData = "Name: $name\n"

                val phoneCur = cr.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + "=?",
                    arrayOf(id), null)
                phoneCur?.let {
                    while (it.moveToNext()) {
                        val phone = it.getColumnIndex(Phone.NUMBER).let { numberIndex -> it.getString(numberIndex) }
                        val type = it.getColumnIndex(Phone.TYPE).let { typeIndex ->
                            it.getInt(typeIndex)
                        }
                        val phoneLabel = context.resources.getString(Phone.getTypeLabelResource(type))
                        logData += "$phoneLabel: $phone\n"
                    }
                    it.close()
                }

                val emailCur = cr.query(Email.CONTENT_URI, null, Email.CONTACT_ID + "=?",
                    arrayOf(id), null)
                emailCur?.let {
                    while (it.moveToNext()) {
                        val email = it.getColumnIndex(Email.ADDRESS).let { emailIndex -> it.getString(emailIndex) }
                        val type = it.getColumnIndex(Email.TYPE).let { typeIndex ->
                            it.getInt(typeIndex)
                        }
                        val emailLabel = context.resources.getString(Email.getTypeLabelResource(type))
                        logData += "$emailLabel: $email\n"
                    }
                    it.close()
                }
                cur?.close()
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
        Text(text = logData)
    }
}
