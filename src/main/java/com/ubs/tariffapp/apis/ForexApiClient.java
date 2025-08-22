package apis;

import models.ForexRate;
import utils.HttpClientUtil;
import org.json.JSONObject;

public class ForexApiClient {
    private static final String API_URL = "https://api.exchangerate.host/latest";

    public static ForexRate getRates(String baseCurrency) throws Exception {
        String url = API_URL + "?base=" + baseCurrency;
        String response = HttpClientUtil.get(url);

        JSONObject json = new JSONObject(response);
        return new ForexRate(baseCurrency, json.getJSONObject("rates").toMap());
    }
}
