package com.imageupload;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {
    @FormUrlEncoded
    @POST("addItem.php")
    Call<JsonObject> add_Item(
            @Field("img") String file
    );
}
