package net.voidbit.fastleds;
import com.loopj.android.http.*;

/**
 * Created by jk on 22.12.2017.
 */

public class RestClient {

    private static String BASE_URL = "192.168.1.220/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
    public static void setBaseUrl(String url) {
         BASE_URL = url;
    }
}
