package android_team.gymme_client.gym.menage_customer;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import android_team.gymme_client.R;
import android_team.gymme_client.customer.CustomerSmallObject;
import android_team.gymme_client.gym.menage_worker.CustomGymTrainerAdapter;
import android_team.gymme_client.gym.menage_worker.GymAddTrainerActivity;
import android_team.gymme_client.gym.menage_worker.GymMenageWorkerActivity;
import android_team.gymme_client.login.LoginActivity;
import android_team.gymme_client.support.MyApplication;
import android_team.gymme_client.trainer.TrainerObject;

public class GymCustomersActivity extends AppCompatActivity {

    private int user_id;

    ListView lv_clienti;
    EditText et_search_client;

    static ArrayList<CustomerSmallObject> lista_clienti;
    static CustomGymCustomerAdapter customer_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gym_customers);

        initialize();

        getCustomers();

        et_search_client.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                GymCustomersActivity.this.customer_adapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });


    }

    private void initialize() {

        // CHECK GET INTENT EXTRAS
        Intent i = getIntent();
        if (!i.hasExtra("user_id")) {
            Toast.makeText(this, "User_id mancante", Toast.LENGTH_LONG).show();
            Intent new_i = new Intent(this, LoginActivity.class);
            startActivity(new_i);
        } else {
            user_id = i.getIntExtra("user_id", -1);
            Log.w("user_id ricevuto:", String.valueOf(user_id));
            if (user_id == -1) {
                Toast.makeText(this, "Utente non creato.", Toast.LENGTH_LONG).show();
                Intent new_i = new Intent(this, LoginActivity.class);
                startActivity(new_i);
            }
        }

        lv_clienti = (ListView) findViewById(R.id.lv_clienti_palestra);
        et_search_client = (EditText) findViewById(R.id.et_search_cliente);

        lista_clienti = new ArrayList<>();


    }

    public static ArrayList<CustomerSmallObject> getAllCustomers(){
        return lista_clienti;
    };

    private void getCustomers() {
        GymCustomersActivity.ReceiveCustomerConnection asyncTaskUser = (GymCustomersActivity.ReceiveCustomerConnection) new GymCustomersActivity.ReceiveCustomerConnection(new GymCustomersActivity.ReceiveCustomerConnection.AsyncResponse() {
            @Override
            public void processFinish(ArrayList<CustomerSmallObject> customers) {
                lista_clienti = customers;
                if (lista_clienti.size() > 0) {
                    //DATI RICEVUTI
                    runOnUiThread(new Runnable() {
                        public void run() {
                            //setto tramite l'adapter la lista dei trainer da visualizzare nella recycler view(notificationView)
                            customer_adapter = new CustomGymCustomerAdapter(GymCustomersActivity.this, lista_clienti);
                            lv_clienti.setAdapter(customer_adapter);

                        }
                    });
                } else {
                    // NESSUN DATO RICEVUTO PERCHE' NESSUNA TRAINER LAVORA PER QUESTA PALESTRA
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(GymCustomersActivity.this, "Nessun cliente per òa tua palestra", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

        }).execute(String.valueOf(user_id));
    }

    private static class ReceiveCustomerConnection extends AsyncTask<String, String, JsonArray> {

        public interface AsyncResponse {
            void processFinish(ArrayList<CustomerSmallObject> customers);
        }

        public GymCustomersActivity.ReceiveCustomerConnection.AsyncResponse delegate = null;

        public ReceiveCustomerConnection(GymCustomersActivity.ReceiveCustomerConnection.AsyncResponse delegate) {
            this.delegate = delegate;
        }

        @SuppressLint("WrongThread")
        @Override
        protected JsonArray doInBackground(String... params) {

            URL url;
            HttpURLConnection urlConnection = null;
            JsonArray _customers = null;
            ArrayList<CustomerSmallObject> t_objects = new ArrayList<CustomerSmallObject>();

            try {
                url = new URL("http://10.0.2.2:4000/gym/get_customers/" + params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                urlConnection.disconnect();

                if (responseCode == HttpURLConnection.HTTP_OK) {

                    Log.e("Server response", "HTTP_OK");
                    String responseString = readStream(urlConnection.getInputStream());
                    _customers = JsonParser.parseString(responseString).getAsJsonArray();

                    for (int i = 0; i < _customers.size(); i++) {
                        JsonObject customer = (JsonObject) _customers.get(i);

                        String user_id = customer.get("user_id").getAsString().trim();
                        String name = customer.get("name").getAsString().trim();
                        String lastname = customer.get("lastname").getAsString().trim();
                        String email = customer.get("email").getAsString().trim();
                        String birthdate = customer.get("birthdate").getAsString().trim();

                        CustomerSmallObject t_obj = new CustomerSmallObject(user_id, name, lastname,email, birthdate);
                        t_objects.add(t_obj);

                    }
                    //SE VA TUTTO A BUON FINE INVIO AL METODO procesFinish();
                    delegate.processFinish(t_objects);

                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Log.e("GET CUSTOMERS", "response: HTTP_NOT_FOUND");
                    delegate.processFinish(new ArrayList<CustomerSmallObject>());
                } else {
                    Log.e("GET CUSTOMERS", "SERVER ERROR");
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("GET CUSTOMERS", "I/O EXCEPTION ERROR");
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return _customers;
        }

        private String readStream(InputStream in) throws UnsupportedEncodingException {
            BufferedReader reader = null;
            StringBuffer response = new StringBuffer();
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line = "";
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return response.toString();
        }
    }


    public static void redirectManage(Activity context) {
        Log.e("REDIRECT", "Gym Menege Worker");
        Intent i = new Intent(context, GymMenageWorkerActivity.class);
        i.putExtra("user_id", Integer.valueOf(GymMenageWorkerActivity.getGymId()));
        context.startActivity(i);
        context.finish();
    }
}