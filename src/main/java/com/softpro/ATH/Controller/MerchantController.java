package com.softpro.ATH.Controller;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.PaymentService;
import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Model.Category;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Payment;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Repository.CategoryRepo;
import com.softpro.ATH.Repository.MerchantRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.PaymentRepo;
import com.softpro.ATH.Repository.ProductRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/Merchant")
public class MerchantController {

	@Autowired
	private ProductRepo productRepo;

	@Autowired
	private MerchantRepo merchantRepo;

	@Autowired
	private OrderRepo orderRepo;

	@Autowired
	private HttpSession session;

	@Autowired
	private CategoryRepo categoryRepo;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private PaymentRepo paymentRepo;

	@Autowired
	private SendAutoEmail sendAutoEmail;

	@GetMapping("/Dashboard")
	public String ShowDashboard(RedirectAttributes attributes, Model model,
			@RequestParam(value = "category", required = false) String categoryName) {
		if (session.getAttribute("loggedInMerchant") == null) {
			attributes.addFlashAttribute("error", "Session Exppired ⚠️");
			return "redirect:/Mlogin";
		}

		List<Category> categories = categoryRepo.findAll();
		List<Product> products;

		if (categoryName != null && !categoryName.isEmpty()) {
			products = productRepo.findByCategoryAndStatus(categoryName, "Available");
			model.addAttribute("selectedCategory", categoryName);
		} else {
			products = productRepo.findByStatus("Available");
		}
		model.addAttribute("categories", categories);
		model.addAttribute("products", products);

		return "Merchant/dashboard";
	}

	@GetMapping("/ViewProfile")
	public String ShowViewProfile(Model model) {
		if (session.getAttribute("loggedInMerchant") != null) {
			Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
			model.addAttribute("merchant", merchant);
			return "Merchant/ViewProfile";
		} else {
			return "redirect:/Mlogin";
		}
	}

	@GetMapping("/EditProfile")
	public String ShowEditProfile(Model model, HttpSession session) {
		if (session.getAttribute("loggedInMerchant") == null) {
			return "redirect:/Mlogin";
		}
		Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
		model.addAttribute("merchant", merchant);
		return "Merchant/EditProfile";
	}

	@PostMapping("/UpdateProfile")
	public String UpdateProfilePic(@RequestParam("file") MultipartFile file,
			@ModelAttribute("merchant") Merchant oldMerchant, RedirectAttributes attributes, HttpSession session) {
		try {
			Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");

			if (!file.isEmpty() && file != null) {
				String storageFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
				String uploadDir = "public/ProfilePic/";
				Path uploadPath = Paths.get(uploadDir);

				if (!Files.exists(uploadPath)) {
					Files.createDirectories(uploadPath);
				}

				// Delete previous profile picture if it exists
				String oldProfilePic = merchant.getProfilepic();
				if (oldProfilePic != null && !oldProfilePic.isEmpty()) {
					Path oldFilePath = Paths.get(uploadDir + oldProfilePic);
					if (Files.exists(oldFilePath)) {
						Files.delete(oldFilePath);
					}
				}

				// Save the new profile picture
				try (InputStream inputStream = file.getInputStream()) {

					Files.copy(inputStream, Paths.get(uploadDir + storageFileName),
							StandardCopyOption.REPLACE_EXISTING);
				}
				merchant.setProfilepic(storageFileName);
			}
			// Update DB
			merchant.setName(oldMerchant.getName());
			merchant.setContactno(oldMerchant.getContactno());
			merchant.setAadharno(oldMerchant.getAadharno());
			merchant.setAddress(oldMerchant.getAddress());
			merchant.setPancardno(oldMerchant.getPancardno());

			merchantRepo.save(merchant);
			attributes.addFlashAttribute("msg", "Profile Successfully Updated");

			return "redirect:/Merchant/EditProfile";

		} catch (Exception e) {
			attributes.addFlashAttribute("msg", "Error : " + e.getMessage());
			return "redirect:/Merchant/EditProfile";
		}
	}

	@GetMapping("/BuyProduct")
	public String ShowBuyProduct(@RequestParam("id") long id, Model model, RedirectAttributes attributes) {
		if (session.getAttribute("loggedInMerchant") == null) {
			attributes.addFlashAttribute("error", "Session Expired ⚠️");
			return "redirect:/Mlogin";
		}

		Product product = productRepo.findById(id).orElseThrow();
		model.addAttribute("razorpayKeyId", "rzp_live_Io1s9ctQtD0G1b");

		model.addAttribute("product", product);
		return "Merchant/BuyProduct";
	}

	@GetMapping("/create-order")
	@ResponseBody
	public Map<String, Object> createRazorpayOrder(@RequestParam long productId, @RequestParam int quantity) {
		Map<String, Object> data = new HashMap<>();
		try {
			Product product = productRepo.findById(productId).orElseThrow();
			int amount = product.getPricePerUnit().multiply(BigDecimal.valueOf(quantity)).intValue();

			com.razorpay.Order razorOrder = paymentService.createRazorpayOrder(amount);

			data.put("orderId", razorOrder.get("id"));
			data.put("razorpayKeyId", "rzp_live_Io1s9ctQtD0G1b");
			data.put("amount", amount * 100); // paise
			data.put("currency", "INR");
		} catch (Exception e) {
			data.put("error", e.getMessage());
		}
		return data;
	}

	@PostMapping("/verify_payment")
	public String verifyPayment(@RequestParam("paymentId") String paymentId,
								@RequestParam("orderId") String razorpayOrderId, 
								@RequestParam("signature") String signature,
								@RequestParam("productId") long productId, 
								@RequestParam("buyQuantity") int quantity, 
								Model model, RedirectAttributes attributes) {
		try {
			Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
			Product product = productRepo.findById(productId).orElseThrow();

			// Save Order
			Order order = new Order();
			order.setProductName(product.getProductName());
			order.setPricePerUnit(product.getPricePerUnit());
			order.setQuantity(quantity);
			order.setFarmer(product.getFarmer());
			order.setMerchant(merchant);
			order.setOrderStatus("Confirmed");
			order.setOrderDate(LocalDateTime.now());
			orderRepo.save(order);

			// Save Payment
			Payment payment = new Payment();
			payment.setOrder(order);

			long transactionId = System.currentTimeMillis();

			payment.setTransactionId(transactionId);
			payment.setPayId(paymentId);
			payment.setAmount(product.getPricePerUnit().multiply(BigDecimal.valueOf(quantity)));
			payment.setPaymentMode("Online");
			payment.setPaymentDate(LocalDateTime.now());

			paymentRepo.save(payment);

			// Update Quantity
			if (product != null) {
				int remainingQty = product.getQuantity() - order.getQuantity();
				if (remainingQty <= 0) {
					remainingQty = 0;
					product.setStatus("OutOfStock");
				}
				product.setQuantity(remainingQty);

				productRepo.save(product);
			}

			sendAutoEmail.SendOrderConfirmationEmail(order);

			attributes.addFlashAttribute("msg", true);
			attributes.addFlashAttribute("transactionId", transactionId);
			attributes.addFlashAttribute("quantity", quantity);

			return "redirect:/Merchant/BuyProduct?id=" + productId;
		} catch (Exception e) {
			attributes.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
			return "redirect:/Merchant/BuyProduct?id=" + productId;
		}
	}

	@GetMapping("/MyOrders")
	public String ShowMyOrders(Model model, RedirectAttributes attributes) {

		if (session.getAttribute("loggedInMerchant") == null) {
			attributes.addFlashAttribute("error", "Session Exppired ⚠️");
			return "redirect:/Mlogin";
		}
		Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
		List<Order> orderList = orderRepo.findByMerchant(merchant);
		model.addAttribute("orderList", orderList.reversed());
		return "Merchant/myorders";
	}

	@GetMapping("/CancelOrder")
	public String ShowCancelOrder(@RequestParam("id") long orderId, RedirectAttributes attributes) {
		if (session.getAttribute("loggedInMerchant") == null) {
			attributes.addFlashAttribute("error", "Session Exppired ⚠️");
			return "redirect:/Mlogin";
		}
		
		try {
			Order order = orderRepo.findByOrderId(orderId);
			Payment payment = paymentRepo.findByOrder(order);
			PaymentService refundService = new PaymentService();
	        refundService.refundPayment(payment.getPayId());

			if (order.getOrderStatus().equals("Confirmed")) {
				sendAutoEmail.SendOrderCancellationEmail(order, payment);
				order.setOrderStatus("Cancelled");
				orderRepo.save(order);
			}
			return "redirect:/Merchant/MyOrders";

		} catch (Exception e) {
			System.err.println("Error : "+e.getMessage());
			return "redirect:/Merchant/MyOrders";
		}
	}

	@GetMapping("/ChangePassword")
	public String ShowChangePassword(RedirectAttributes attributes) {
		if (session.getAttribute("loggedInMerchant") == null) {
			attributes.addFlashAttribute("error", "Session Exppired ⚠️");
			return "redirect:/Mlogin";
		}

		return "Merchant/ChangePassword";
	}

	@PostMapping("/ChangePassword")
	public String ChangePassword(HttpServletRequest request, RedirectAttributes attributes) {
		try {
			// Get logged-in merchant from session
			Merchant merchant = (Merchant) session.getAttribute("loggedInMerchant");
			String newPassword = request.getParameter("newpass");
			String confirmPassword = request.getParameter("confirmpass");
			String oldPassword = request.getParameter("oldpass");

			if (newPassword.equals(oldPassword)) {
				attributes.addFlashAttribute("error", "New password & Old Password is same, please try different.");
				return "redirect:/Merchant/ChangePassword";
			}
			// Check if new password and confirm password match
			if (!newPassword.equals(confirmPassword)) {
				attributes.addFlashAttribute("error", "New password and Confirm Password do not match.");
				return "redirect:/Merchant/ChangePassword";
			}

			// Check if current password matches
			if (!merchant.getPassword().equals(oldPassword)) {
				attributes.addFlashAttribute("error", "Invalid Old Password.");
				return "redirect:/Merchant/ChangePassword";
			}

			merchant.setPassword(confirmPassword);
			merchantRepo.save(merchant);
			session.invalidate();
			attributes.addFlashAttribute("msg", "Password Successfully Changed");
			return "redirect:/Mlogin";

		} catch (Exception e) {
			// TODO: handle exception
			attributes.addFlashAttribute("error", "Error : " + e.getMessage());
			return "redirect:/Merchant/ChangePassword";
		}
	}

	@GetMapping("/Logout")
	public String Logout(RedirectAttributes attributes) {
		if (session.getAttribute("loggedInMerchant") == null) {
			attributes.addFlashAttribute("error", "Session Exppired ⚠️");
			return "redirect:/Mlogin";
		}

		session.removeAttribute("loggedInMerchant");
		attributes.addFlashAttribute("msg", "Logout Successful ✅");
		return "redirect:/Mlogin";
	}
}
