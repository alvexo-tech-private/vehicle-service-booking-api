package com.alvexo.bookingapp.util;
import java.io.IOException;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.*;
import com.sendgrid.helpers.mail.objects.*;

public class EmailTester {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		

		Email from = new Email("info@alvexotech.com");
		String subject = "Service Booking Confirmation";

		Personalization personalization = new Personalization();
		personalization.addTo(new Email("saravananmcase@gmail.com"));
		personalization.addTo(new Email("info@alvexotech.com"));

		Content content = new Content("text/plain", "Your booking is confirmed.");

		Mail mail = new Mail();
		mail.setFrom(from);
		mail.setSubject(subject);
		mail.addContent(content);
		mail.addPersonalization(personalization);

		SendGrid sg = new SendGrid("");
		Request request = new Request();

		request.setMethod(Method.POST);
		request.setEndpoint("mail/send");
		request.setBody(mail.build());

		Response response = sg.api(request);
		System.out.println("Response "+ response.getStatusCode());
	}


}
