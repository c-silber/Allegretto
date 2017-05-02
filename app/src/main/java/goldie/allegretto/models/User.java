package goldie.allegretto.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
        private String first_name;
        private String last_name;
        private String email;
        private String unique_id;

    public String getName(){
         return first_name + " " + last_name;
    }

    public String getEmail(){
        return email;
    }

        // Default constructor
        public User(){}

        public User(String first_name, String last_name, String email){
        this.first_name = first_name;
        this.last_name = last_name;
        this.email = email;
    }
}