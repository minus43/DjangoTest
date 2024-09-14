package org.techtown.djangotest;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface JsonPlaceHolderApi {

    @Multipart
    @POST("api/images/upload/")
    Call<ResponseBody> uploadImage(
            @Part MultipartBody.Part image,
            @Part("description") RequestBody description
    );

    @GET("api/images/latest/")
    Call<String> getLatestImageUrl(); // 최신 이미지 URL을 반환하는 엔드포인트
}