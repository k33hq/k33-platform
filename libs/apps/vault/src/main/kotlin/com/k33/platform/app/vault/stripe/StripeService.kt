package com.k33.platform.app.vault.stripe

import com.k33.platform.utils.stripe.StripeClient
import com.stripe.model.Address
import com.stripe.model.Customer
import com.stripe.param.CustomerListParams

object StripeService {

    private val stripeClient by lazy {
        StripeClient(System.getenv("VAULT_STRIPE_API_KEY"))
    }

    suspend fun getAllCustomerEmails(): List<String> {
        val listParams = CustomerListParams
            .builder()
            .setLimit(100)
            .build()

        return stripeClient.call {
            Customer.list(listParams, requestOptions)
        }
            ?.data
            ?.map { it.email }
            ?.distinct()
            ?.toList()
            ?: emptyList()
    }

    data class CustomerDetails(
        val name: String,
        val address: Address,
        val email: String,
    )

    suspend fun getAllCustomerDetails(): List<CustomerDetails> {
        val listParams = CustomerListParams
            .builder()
            .setLimit(100)
            .build()

        return stripeClient.call {
            Customer.list(listParams, requestOptions)
        }
            ?.data
            ?.map {
                CustomerDetails(
                    name = it.name,
                    address = it.address,
                    email = it.email,
                )
            }
            ?: emptyList()
    }

    suspend fun getCustomerDetails(
        email: String,
    ): List<CustomerDetails> {
        val listParams = CustomerListParams
            .builder()
            .setEmail(email)
            .setLimit(100)
            .build()

        return stripeClient.call {
            Customer.list(listParams, requestOptions)
        }
            ?.data
            ?.map {
                CustomerDetails(
                    name = it.name,
                    address = it.address,
                    email = it.email,
                )
            }
            ?: emptyList()
    }
}