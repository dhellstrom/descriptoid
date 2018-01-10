import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Sends POST-requests to the Langforia API. Used to annotate text. Can request results on both
 * binary and json format.
 */
public class HttpRequester {

    public static String requestJSON(String query) {
        StringBuffer data = new StringBuffer();
        String line;
        try {
            InputStream stream = sendPost(query, "json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data.toString();
    }

    public static byte[] requestBinary(String query) {
        byte[] data = new byte[4096];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;

        try {
            InputStream stream = sendPost(query, "binary");
            while ((nRead = stream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer.toByteArray();
    }


	private static InputStream sendPost(String query, String returnType) throws Exception {
        String url = "http://vilde.cs.lth.se:9000/en/default/api/" + returnType;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        con.setRequestMethod("POST");

        con.setRequestProperty("Content-Type", "text/plain; charset=utf-8");

        // Send post request
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(query.getBytes("UTF-8"));
        os.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        return con.getInputStream();
    }
}
