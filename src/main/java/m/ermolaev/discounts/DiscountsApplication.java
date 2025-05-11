package m.ermolaev.discounts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import m.ermolaev.discounts.model.Order;
import m.ermolaev.discounts.model.PaymentMethod;

import java.io.File;
import java.util.List;

public class DiscountsApplication {
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: java -jar app.jar /path/to/orders.json /path/to/paymentmethods.json");
			System.exit(1);
		}

		ObjectMapper mapper = new ObjectMapper();

		List<Order> orders = mapper.readValue(new File(args[0]), new TypeReference<>() {});
		List<PaymentMethod> paymentMethods = mapper.readValue(new File(args[1]), new TypeReference<>() {});

		// TODO: call the service
	}
}