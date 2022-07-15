package info.kalagato.com.extractor.networkCall


import com.google.gson.annotations.SerializedName

data class DetailInfoResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: String
) {
    data class Data(
        @SerializedName("message")
        val message: String
    )
}