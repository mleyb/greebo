package com.bluezero.greebo;

import android.content.Context;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceUser;

import java.net.MalformedURLException;

public class MobileServiceClientFactory {
    public static MobileServiceClient createAzureClient(Context context, MobileServiceUser user) {

        MobileServiceClient client = null;

        try {
            // create the client, attach the auth filter and set the current user
            client = new MobileServiceClient(Constants.ServiceUri, Constants.ApiKey, context);
            //client = client.withFilter(new AuthenticationFilter(client));

            //if (user != null) {
                //client.setCurrentUser(user);
            //}
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return client;
    }
}
