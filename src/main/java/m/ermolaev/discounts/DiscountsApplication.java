package m.ermolaev.discounts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import m.ermolaev.discounts.model.Order;
import m.ermolaev.discounts.model.PaymentMethod;
import m.ermolaev.discounts.model.PaymentResult;
import m.ermolaev.discounts.service.PaymentOptimizerService;

import java.io.File;
import java.util.List;

public class DiscountsApplication {
	public static void main(String[] args) {
		try {
			if (args.length != 2) {
				System.err.println("Usage: java -jar app.jar /path/to/orders.json /path/to/paymentmethods.json");
				System.exit(1);
			}

			File ordersFile = new File(args[0]);
			if (!ordersFile.exists()) {
				System.err.println("orders.json not found: " + ordersFile.getAbsolutePath());
				System.exit(1);
			}

			File paymentsFile = new File(args[0]);
			if (!paymentsFile.exists()) {
				System.err.println("payments.json not found: " + paymentsFile.getAbsolutePath());
				System.exit(1);
			}

			ObjectMapper mapper = new ObjectMapper();
			List<Order> orders = mapper.readValue(new File(args[0]), new TypeReference<>() {
			});
			List<PaymentMethod> paymentMethods = mapper.readValue(new File(args[1]), new TypeReference<>() {
			});
			PaymentOptimizerService service = new PaymentOptimizerService();
			List<PaymentResult> result = service.optimize(orders, paymentMethods);

			for (PaymentResult pr : result) {
				System.out.println(pr.getMethodId() + " " + pr.getAmount());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}