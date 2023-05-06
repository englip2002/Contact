package my.edu.tarc.contact

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import my.edu.tarc.contact.databinding.FragmentFirstBinding
import my.edu.tarc.mycontact.WebDB
import my.tarc.mycontact.Contact
import my.tarc.mycontact.ContactAdapter
import my.tarc.mycontact.ContactViewModel
import my.tarc.mycontact.RecordClickListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.UnknownHostException

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), MenuProvider, RecordClickListener {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //Refer to the ViewModel created by the Main Activity
    private val myContactViewModel: ContactViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        //handle menu click event
        val menuHost: MenuHost = this.requireActivity()
        menuHost.addMenuProvider(
            this, viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ContactAdapter(this)

        //Add an observer
        myContactViewModel.contactList.observe(
            viewLifecycleOwner,
            Observer {
                if (it.isEmpty()) {
                    binding.textViewCount.isVisible = true
                    binding.textViewCount.text = getString(R.string.no_record)
                } else {
                    binding.textViewCount.isVisible = false
                }
                adapter.setContact(it)
            }
        )
        binding.recyclerView.adapter = adapter

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        //menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_upload) {
            //open shared preference
            val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val userRef = sharedPref.getString(getString(R.string.phone),"")
            if(userRef.isNullOrEmpty()){
                Toast.makeText(context, getString(R.string.error_profile), Toast.LENGTH_SHORT).show()
            }else{
                if (myContactViewModel.contactList.isInitialized) {
                    val database =
                        Firebase.database("https://contact-ef549-default-rtdb.asia-southeast1.firebasedatabase.app").reference
                    myContactViewModel.contactList.value!!.forEach{
                        database.child(userRef).child(it.phone).setValue(it)
                    }
                    Toast.makeText(context, "Uploaded", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // for download data from server
        if(menuItem.itemId == R.id.action_download){
            downloadContact(requireContext(), getString(R.string.url_server)+ getString(R.string.url_get_all))
            return true
        }
        return false
    }

    //tell the second fragment to know user clicked which one
    override fun onRecordClickListener(index: Int) {
        //selectedIndex from viewModel
        myContactViewModel.selectedIndex = index
        findNavController().navigate(R.id.nav_second)
    }

    // to download contact from server (not applicable to firebase)
    fun downloadContact(context: Context, url: String){
        binding.progressBar.isVisible = true
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                // Process the JSON
                try {
                    if (response != null) {
                        val strResponse = response.toString()
                        val jsonResponse = JSONObject(strResponse)
                        val jsonArray: JSONArray = jsonResponse.getJSONArray("records")
                        val size: Int = jsonArray.length()

                        if(myContactViewModel.contactList.value?.isNotEmpty()!!){
                            myContactViewModel.deleteAll()
                        }

                        for (i in 0..size - 1) {
                            var jsonContact: JSONObject = jsonArray.getJSONObject(i)
                            var contact = Contact(
                                jsonContact.getString("name"),
                                jsonContact.getString("contact")
                            )
                            myContactViewModel.addContact(Contact(contact?.name!!, contact?.phone!!))
                        }
                        Toast.makeText(context, "$size record(s) downloaded", Toast.LENGTH_SHORT).show()
                        binding.progressBar.isVisible = false
                    }
                }catch (e: UnknownHostException){
                    Log.d("ContactRepository", "Unknown Host: %s".format(e.message.toString()))
                    binding.progressBar.isVisible = false
                }
                catch (e: Exception) {
                    Log.d("ContactRepository", "Response: %s".format(e.message.toString()))
                    binding.progressBar.isVisible = false
                }
            },
            { error ->
                Log.d("ContactRepository", "Error Response: %s".format(error.message.toString()))
            },
        )

        //Volley request policy, only one time request
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
            0, //no retry
            1f
        )

        // Access the RequestQueue through your singleton class.
        WebDB.getInstance(context).addToRequestQueue(jsonObjectRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}