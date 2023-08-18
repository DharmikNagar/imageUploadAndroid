package com.imageupload;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitInstance {
    String url = "Your Api";
    public static RetrofitInstance retrofitInstance;
    public static ApiInterface apiInterface;
    RetrofitInstance() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiInterface = retrofit.create(ApiInterface.class);
    }

    public static RetrofitInstance getRetrofitInstance(){
        if(retrofitInstance == null){
            retrofitInstance = new RetrofitInstance();
        }

        return retrofitInstance;
    }
}