package my.edu.tarc.contact

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import my.edu.tarc.contact.databinding.FragmentSecondBinding
import my.tarc.mycontact.Contact
import my.tarc.mycontact.ContactViewModel

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 * adding the new contact
 */
class SecondFragment : Fragment(), MenuProvider {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //Refer to the View model created by the Main Activity
    val myContactViewModel: ContactViewModel by activityViewModels()

    //use in onViewCreated
    private var isEditing: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        //Let ProfileFragment to manage the Menu
        val menuHost: MenuHost = this.requireActivity()
        menuHost.addMenuProvider(
            this, viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //indicate which view user is looking at (view mode - edit? new?)
        //if != -1, isEditing = True
        isEditing = myContactViewModel.selectedIndex != -1
        if (isEditing == true) {
            with(binding) {
                //value!! is for the current contactList information
                val contact: Contact =
                    myContactViewModel.contactList.value!!.get(myContactViewModel.selectedIndex)
                //to pass the current value to the edit text
                editTextName.setText(contact.name)
                editTextPhone.setText(contact.phone)
                editTextName.requestFocus()
                editTextPhone.isEnabled = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //to reset the mode when user the view is destroyed
        myContactViewModel.selectedIndex = -1

    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.second_menu, menu)
//        menu.findItem(R.id.action_settings).isVisible = false
        //show delete icon when it is in edit mode
        menu.findItem(R.id.action_delete).isVisible = isEditing

    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_save) {
            //Insert a new contact to the Database
            binding.apply {
                val name = editTextName.text.toString()
                val phone = editTextPhone.text.toString()
                val newContact = Contact(name, phone)
                if (isEditing) {
                    myContactViewModel.updateContact(newContact)
                } else {
                    myContactViewModel.addContact(newContact)
                }
            }
            Toast.makeText(context, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        } else if (menuItem.itemId == R.id.action_delete) {
            val builder = AlertDialog.Builder(requireActivity())
            builder.setMessage(getString(R.string.delete_record))
                .setPositiveButton(getString(R.string.delete), { _, _ ->
                    val contact = myContactViewModel.contactList.value!!.get(myContactViewModel.selectedIndex)
                    myContactViewModel.deleteContact(contact)
                    findNavController().navigateUp()
                }).setNegativeButton(getString(R.string.cancel), { _, _ ->
                    //Do nothing here
                })
            builder.create().show()

        } else if (menuItem.itemId == android.R.id.home) {
            findNavController().navigateUp()
        }

        return true
    }

}