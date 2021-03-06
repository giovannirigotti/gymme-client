package android_team.gymme_client.customer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

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
import android_team.gymme_client.gym.manage_course.CourseObject;
import android_team.gymme_client.login.LoginActivity;
import android_team.gymme_client.support.Drawer;

public class CustomerAddCourseActivity extends AppCompatActivity {

    static CustomDisponibleCourseAdapter course_adapter;
    private static int user_id;
    static ListView lv_course;
    EditText inputSearch;
    public static ArrayList<CourseObject> course_list;
    DrawerCustomerListener drawerCustomerListener;
    DrawerLayout drawerLayout;
    TextView tv_title, no_item_cus_add_course;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_add_course);

        course_list = new ArrayList<CourseObject>();

        Intent i = getIntent();
        if (!i.hasExtra("user_id")) {
            Toast.makeText(this, "User_id mancante", Toast.LENGTH_LONG).show();
            Intent new_i = new Intent(this, LoginActivity.class);
            startActivity(new_i);
        } else {
            user_id = i.getIntExtra("user_id", -1);
            Log.w("user_id ricevuto:", String.valueOf(user_id));
            if (user_id == -1) {
                Toast.makeText(this, "Utente non creato", Toast.LENGTH_LONG).show();
                Intent new_i = new Intent(this, LoginActivity.class);
                startActivity(new_i);
            }
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_home_activity);
        drawerCustomerListener = new DrawerCustomerListener(this, user_id);
        tv_title = (TextView) findViewById(R.id.main_toolbar_title);
        no_item_cus_add_course = (TextView) findViewById(R.id.no_item_cus_add_course);

        tv_title.setText("Aggiungi corso");

        lv_course = (ListView) findViewById(R.id.lv_customer_disponible_course);

        getCourse();

        inputSearch = (EditText) findViewById(R.id.et_search_disponible_courses);

        if (!course_list.isEmpty()) {
            inputSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                    // When user changed the Text
                    CustomerAddCourseActivity.this.course_adapter.getFilter().filter(cs);
                }

                @Override
                public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                }

                @Override
                public void afterTextChanged(Editable arg0) {
                }
            });
        }
    }

    private void getCourse() {
        CustomerAddCourseActivity.ReceiveCourseConn asyncTaskUser = (CustomerAddCourseActivity.ReceiveCourseConn) new CustomerAddCourseActivity.ReceiveCourseConn(new CustomerAddCourseActivity.ReceiveCourseConn.AsyncResponse() {
            @Override
            public void processFinish(ArrayList<CourseObject> courses) {

                course_list = courses;
                if (course_list.size() > 0) {
                    //DATI RICEVUTI
                    runOnUiThread(new Runnable() {
                        public void run() {
                            //setto tramite l'adapter la lista dei trainer da visualizzare nella recycler view(notificationView)
                            course_adapter = new CustomDisponibleCourseAdapter(CustomerAddCourseActivity.this, course_list);
                            lv_course.setAdapter(course_adapter);

                        }
                    });
                } else {
                    // NESSUN DATO RICEVUTO PERCHE' NESSUNA TRAINER LAVORA PER QUESTA PALESTRA
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(CustomerAddCourseActivity.this, "Nessun corso", Toast.LENGTH_SHORT).show();
                            no_item_cus_add_course.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }

        }).execute(String.valueOf(user_id));
    }

    public static ArrayList<CourseObject> getAllCourses() {
        return course_list;
    }

    private static class ReceiveCourseConn extends AsyncTask<String, String, JsonArray> {

        public interface AsyncResponse {
            void processFinish(ArrayList<CourseObject> courses);
        }

        public CustomerAddCourseActivity.ReceiveCourseConn.AsyncResponse delegate = null;

        public ReceiveCourseConn(CustomerAddCourseActivity.ReceiveCourseConn.AsyncResponse delegate) {
            this.delegate = delegate;
        }

        @SuppressLint("WrongThread")
        @Override
        protected JsonArray doInBackground(String... params) {

            URL url;
            HttpURLConnection urlConnection = null;
            JsonArray _courses = null;
            ArrayList<CourseObject> c_objects = new ArrayList<>();

            try {
                url = new URL("http://10.0.2.2:4000/customer/get_disponible_course_customers/" + params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);
                urlConnection.connect();
                int responseCode = urlConnection.getResponseCode();
                urlConnection.disconnect();

                if (responseCode == HttpURLConnection.HTTP_OK) {

                    //Log.e("Server response", "HTTP_OK");
                    String responseString = readStream(urlConnection.getInputStream());
                    _courses = JsonParser.parseString(responseString).getAsJsonArray();

                    for (int i = 0; i < _courses.size(); i++) {
                        JsonObject course = (JsonObject) _courses.get(i);
                        String course_id = course.get("course_id").getAsString().trim();
                        String name = course.get("name").getAsString().trim();
                        String lastname = course.get("lastname").getAsString().trim();
                        String description = course.get("description").getAsString().trim();
                        String title = course.get("title").getAsString().trim();
                        String category = course.get("category").getAsString().trim();
                        String start_date = course.get("start_date").getAsString().trim();
                        String end_date = course.get("end_date").getAsString().trim();
                        String max_persons = course.get("max_persons").getAsString().trim();

                        CourseObject c_obj = new CourseObject(course_id, name, lastname, description, title, category, start_date, end_date, max_persons);
                        c_objects.add(c_obj);

                    }
                    //SE VA TUTTO A BUON FINE INVIO AL METODO procesFinish();
                    delegate.processFinish(c_objects);

                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    //Log.e("GET TRAINER", "response: HTTP_NOT_FOUND");
                    delegate.processFinish(new ArrayList<CourseObject>());
                } else {
                    //Log.e("GET TRAINER", "SERVER ERROR");
                }
            } catch (IOException e) {
                e.printStackTrace();
                //Log.e("GET TRAINER", "I/O EXCEPTION ERROR");
            } finally {
                if (urlConnection != null)
                    urlConnection.disconnect();
            }
            return _courses;
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

    public static String getUserId() {
        return String.valueOf(user_id);
    }

    public static void redoAdapterCourse(Activity context, ArrayList<CourseObject> courses, Integer position) {
        ArrayList<CourseObject> new_t = new ArrayList<>();
        for (int i = 0; i < courses.size(); i++) {
            if (i != position) {
                new_t.add(courses.get(i));
            }
        }
        course_adapter = new CustomDisponibleCourseAdapter(context, new_t);
        lv_course.setAdapter(course_adapter);
    }

    //region DRAWER
    @Override
    protected void onPause() {
        super.onPause();
        Drawer.closeDrawer(drawerLayout);
    }

    public void ClickMenu(View view) {
        Drawer.openDrawer(drawerLayout);
    }

    public void ClickDrawer(View view) {
        Drawer.closeDrawer(drawerLayout);
    }

    public void customerToNotify(View view) {
        drawerCustomerListener.toNotify();
    }

    public void customerToTrainings(View view) {
        drawerCustomerListener.toTrainings();
    }

    public void customerToGym(View view) {
        drawerCustomerListener.toGym();
    }

    public void customerToCourse(View view) {
        drawerCustomerListener.toCourse();
    }

    public void customerToProfile(View view) {
        drawerCustomerListener.toProfile();
    }

    public void customerToHome(View view) {
        drawerCustomerListener.toHome();
    }
//endregion
}