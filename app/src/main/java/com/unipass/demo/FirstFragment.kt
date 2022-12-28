package com.unipass.demo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.unipass.core.UniPassSDK
import com.unipass.core.types.LoginOutput
import com.unipass.core.types.Network
import com.unipass.core.types.UniPassSDKOptions
import com.unipass.demo.databinding.FragmentFirstBinding
import java8.util.concurrent.CompletableFuture

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)


            if(activity != null) {
                var sdk = UniPassSDK(
                    UniPassSDKOptions(
                        context = activity!!,
                        redirectUrl = Uri.parse("unipassapp://com.unipass.wallet/redirect"),
                        network = Network.TESTNET
                    )
                )


                val loginCompletableFuture: java.util.concurrent.CompletableFuture<LoginOutput> = sdk.login()

                loginCompletableFuture.whenComplete { loginResponse, error ->
                    if (error == null) {
                        Log.d("MainActivity_Web3Auth", "success")
                    } else {
                        Log.d("MainActivity_Web3Auth", error.message ?: "Something went wrong")
                    }
                }
            }


        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}