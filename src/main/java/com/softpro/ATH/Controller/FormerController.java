package com.softpro.ATH.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.PaymentService;
import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Dto.ProductDto;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Payment;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Repository.CategoryRepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.PaymentRepo;
import com.softpro.ATH.Repository.ProductRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@ControllerAdvice
@RequestMapping("/Former")
public class FormerController {

	@Autowired
	private HttpSession session;

	@Autowired
	FormersRepo formersRepo;

	@Autowired
	ProductRepo productRepo;

	@Autowired
	CategoryRepo categoryRepo;

	@Autowired
	OrderRepo orderRepo;

	@Autowired
	private PaymentRepo paymentRepo;
	
	@Autowired
	private SendAutoEmail sendAutoEmail;
	
	@Autowired
	private PaymentService paymentService;
	
	@GetMapping("/Dashboard")
	public String ShowDasboard(Model model) {
		if (session.getAttribute("loggedInFormer") != null) {
			model.addAttribute("active1", "active");
			Formers farmer = (Formers) session.getAttribute("loggedInFormer");
			List<Order> recentOrders = orderRepo.findTop5ByFarmerOrderByOrderDateDesc(farmer);

			model.addAttribute("totalProducts", productRepo.countByFarmer(farmer));
			model.addAttribute("totalOrders", orderRepo.countByFarmer(farmer));
			model.addAttribute("completedOrders", orderRepo.countByFarmerAndOrderStatus(farmer, "Delivered"));
			model.addAttribute("cancelledOrders", orderRepo.countByFarmerAndOrderStatus(farmer, "Cancelled"));
			
			model.addAttribute("recentOrders", recentOrders);
			
			model.addAttribute("totalRevenue", orderRepo.getTotalRevenueByFarmerId(farmer.getId()));
			model.addAttribute("monthlyRevenue", orderRepo.getCurrentMonthRevenue(farmer.getId()));
			model.addAttribute("topProduct", orderRepo.getMostOrderedProduct(farmer.getId()));
			
			BigDecimal inStockRevenue = productRepo.calculateInStockRevenue(farmer.getId());
			model.addAttribute("inStockRevenue", inStockRevenue != null ? inStockRevenue : BigDecimal.ZERO);

			

			return "Former/dashboard";
		} else {
			return "redirect:/Flogin";
		}
	}

	@GetMapping("/EditProfile")
	public String ShowEditProfile(Model model, HttpSession session) {
		if (session.getAttribute("loggedInFormer") == null) {
			return "redirect:/Flogin";
		}
		Formers farmer = (Formers) session.getAttribute("loggedInFormer");
		model.addAttribute("former", farmer);
		return "Former/EditProfile";
	}

	@PostMapping("/UpdateProfile")
	public String UpdateProfilePic(@RequestParam("file") MultipartFile file,
			@ModelAttribute("former") Formers oldFormer, RedirectAttributes attributes, HttpSession session) {
		try {
			Formers farmer = (Formers) session.getAttribute("loggedInFormer");

			if (!file.isEmpty() && file != null) {
				String storageFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
				String uploadDir = "public/ProfilePic/";
				Path uploadPath = Paths.get(uploadDir);

				if (!Files.exists(uploadPath)) {
					Files.createDirectories(uploadPath);
				}

				// Delete previous profile picture if it exists
				String oldProfilePic = farmer.getProfilepic();
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
				farmer.setProfilepic(storageFileName);
			}
			// Update DB
			farmer.setName(oldFormer.getName());
			farmer.setContactno(oldFormer.getContactno());
			farmer.setAadharno(oldFormer.getAadharno());
			farmer.setAddress(oldFormer.getAddress());
			formersRepo.save(farmer);
			return "redirect:/Former/EditProfile";

		} catch (Exception e) {
			attributes.addFlashAttribute("msg", "Error : " + e.getMessage());
			return "redirect:/Former/EditProfile";
		}
	}

	@GetMapping("/AddProduct")
	public String ShowAddProduct(Model model) {

		if (session.getAttribute("loggedInFormer") != null) {

			model.addAttribute("productDto", new ProductDto());
			model.addAttribute("categories", categoryRepo.findAll());
			model.addAttribute("active2", "active");
			return "Former/addproduct";
		} else {
			return "redirect:/Flogin";
		}
	}

	@PostMapping("/AddProduct")
	public String AddProduct(@ModelAttribute ProductDto dto, RedirectAttributes attributes) {
		try {
			Product product = new Product();
			Formers former = (Formers) session.getAttribute("loggedInFormer");

			MultipartFile file = dto.getImage();
			String storageFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
			String uploadDir = "Public/ProductImage/";
			Path uploadPath = Paths.get(uploadDir);

			if (!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}

			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
			}

			product.setProductName(dto.getProductName());
			product.setCategory(dto.getCategory());
			product.setQuantity(dto.getQuantity());
			product.setPricePerUnit(dto.getPricePerUnit());
			product.setImage(storageFileName);
			product.setStatus("Available");
			product.setFarmer(former);

			productRepo.save(product);
			attributes.addFlashAttribute("msg", "Product Added Successfully");
			return "redirect:/Former/AddProduct";
		} catch (Exception e) {
			return "redirect:/Former/AddProduct";
		}
	}

	@GetMapping("/ManageProduct")
	public String ShowManageProduct(Model model, @RequestParam(name = "status", required = false) String status) {
		if (session.getAttribute("loggedInFormer") != null) {
			model.addAttribute("active3", "active");
			Formers farmer = (Formers) session.getAttribute("loggedInFormer");

			List<Product> productList;

			if (status != null && !status.isEmpty()) {
				productList = productRepo.findByFarmerAndStatus(farmer, status);
			} else {
				productList = productRepo.findByFarmer(farmer);
			}

			model.addAttribute("productList", productList);
			model.addAttribute("selectedStatus", status); // for keeping selected option
			return "Former/manageproduct";
		} else {
			return "redirect:/Flogin";
		}
	}

	@GetMapping("/EditProduct")
	public String ShowEditProduct(@RequestParam("id") long id, Model model) {
		if (session.getAttribute("loggedInFormer") != null) {
			Product product = productRepo.findById(id).get();
			model.addAttribute("product", product);
			model.addAttribute("categories", categoryRepo.findAll());

			return "Former/EditProduct";
		} else {
			return "redirect:/Flogin";
		}
	}

	@GetMapping("/DeleteProduct")
	public String ShowDeleteProduct(@RequestParam("id") long id, Model model) {
		if (session.getAttribute("loggedInFormer") != null) {
			try {
				// 1. Get the product by ID
				Optional<Product> optionalProduct = productRepo.findById(id);
				if (optionalProduct.isPresent()) {
					Product product = optionalProduct.get();

					// 2. Delete the image file
					String imagePath = "Public/ProductImage/" + product.getImage();
					Path imageFile = Paths.get(imagePath);
					try {
						Files.deleteIfExists(imageFile); // Delete the image if it exists
					} catch (IOException e) {
						e.printStackTrace(); // Optionally log this
					}

					// 3. Delete the product from DB
					productRepo.deleteById(id);
				}
				return "redirect:/Former/ManageProduct";
			} catch (Exception e) {
				return "redirect:/Former/ManageProduct";
			}
		} else {
			return "redirect:/Flogin";
		}
	}

	@PostMapping("/EditProduct")
	public String EditProduct(@ModelAttribute ProductDto dto, @RequestParam("file") MultipartFile file,
			RedirectAttributes attributes) {
		try {
			Product product = productRepo.findById(dto.getId()).get();

			if (!file.isEmpty() && file != null) {
				String storageFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
				String uploadDir = "Public/ProductImage/";
				Path uploadPath = Paths.get(uploadDir);

				if (!Files.exists(uploadPath)) {
					Files.createDirectories(uploadPath);
				}

				try (InputStream inputStream = file.getInputStream()) {
					Files.copy(inputStream, Paths.get(uploadDir + storageFileName),
							StandardCopyOption.REPLACE_EXISTING);
				}
				product.setImage(storageFileName);
			}

			product.setProductName(dto.getProductName());
			product.setCategory(dto.getCategory());
			product.setQuantity(dto.getQuantity());
			product.setPricePerUnit(dto.getPricePerUnit());
			if (dto.getQuantity() > 0) {
				product.setStatus("Available");
			}
			productRepo.save(product);
			attributes.addFlashAttribute("success", "Product Successfully Updated");
			return "redirect:/Former/EditProduct?id=" + product.getId();
		} catch (Exception e) {
			attributes.addFlashAttribute("error", "Error : " + e.getMessage());
			return "redirect:/Former/EditProduct?id=" + dto.getId();
		}
	}
	
	//Get Sorted order or Product
	public List<Order> getSortedOrdersForFarmer(Formers farmer) {
	    List<Order> orders = orderRepo.findByFarmerOrdered(farmer);

	    orders.sort((o1, o2) -> {
	        int statusRank1 = getStatusRank(o1.getOrderStatus());
	        int statusRank2 = getStatusRank(o2.getOrderStatus());

	        if (statusRank1 != statusRank2) {
	            return Integer.compare(statusRank1, statusRank2); // Lower rank first
	        }

	        // If status is same, sort by date (latest first)
	        return o2.getOrderDate().compareTo(o1.getOrderDate());
	    });

	    return orders;
	}

	private int getStatusRank(String status) {
	    if (status == null) return 4; // null-safe

	    return switch (status.toLowerCase()) {
	        case "confirmed" -> 1;
	        case "delivered" -> 2;
	        case "cancelled" -> 3;
	        default -> 4; // Unknown at bottom
	    };
	}


	@GetMapping("/Orders")
	public String ShowViewOrders(Model model) {
		if (session.getAttribute("loggedInFormer") != null) {
			model.addAttribute("active4", "active");

			Formers farmer = (Formers) session.getAttribute("loggedInFormer");
			// List<Order> orders = orderRepo.findByFarmer(farmer);
			List<Order> sortedOrders = getSortedOrdersForFarmer(farmer);
			model.addAttribute("orders", sortedOrders);
			return "Former/vieworders";
		} else {
			return "redirect:/Flogin";
		}
	}

	@GetMapping("ViewPaymentSlip")
	public String viewPaymentSlip(@RequestParam("id") Long orderId, Model model, RedirectAttributes attributes) {
		if (session.getAttribute("loggedInFormer") == null) {
			return "redirect:/Flogin";
		}
		
		try {
			Order order = orderRepo.findByOrderId(orderId);
		    if (order==null) {
		        return "redirect:/Former/Orders";
		    }

		    Payment payment = paymentRepo.findByOrder(order);
		    if (payment==null) {
		    	attributes.addFlashAttribute("msg", "No Payment Records Found");
		        return "redirect:/Former/Orders";
		    }
			
			 model.addAttribute("order", order); 
			 model.addAttribute("payment", payment);
			 return "Former/ViewPaymentSlip"; // name of the Thymeleaf template

		} catch (Exception e) {
			attributes.addFlashAttribute("msg", "Error : "+e.getMessage());
	        return "redirect:/Former/Orders";
		}
	}

	@PostMapping("/updateOrderStatus")
	public String updateOrderStatus(@RequestParam("orderId") Long orderId, @RequestParam("newStatus") String newStatus,
			RedirectAttributes redirectAttributes) {

		try {
			Order order = orderRepo.findByOrderId(orderId);
			Payment payment = paymentRepo.findByOrder(order);
			if (order.getOrderStatus().equals("Delivered")) {
				redirectAttributes.addFlashAttribute("warning", true);
				return "redirect:/Former/Orders";
			}

			if (order.getOrderStatus().equals("Cancelled")) {
				redirectAttributes.addFlashAttribute("Cancelled", true);
				return "redirect:/Former/Orders";
			}
			if (newStatus.equals("Delivered")) {
				order.setDeliveredDate(LocalDateTime.now());
			}
			if (newStatus.equals("Cancelled")) {
				sendAutoEmail.SendOrderCancellationEmail(order, payment);
				paymentService.refundPayment(payment.getPayId());
			}

			order.setOrderStatus(newStatus);
			
			orderRepo.save(order);
			redirectAttributes.addFlashAttribute("success", true);
			return "redirect:/Former/Orders";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", false);
			return "redirect:/Former/Orders";
		}
	}

	@GetMapping("/UserProfile")
	public String ShowViewProfile(Model model) {
		if (session.getAttribute("loggedInFormer") != null) {
			model.addAttribute("active6", "active");
			Formers formers = (Formers) session.getAttribute("loggedInFormer");
			model.addAttribute("former", formers);
			return "Former/viewprofile";
		} else {
			return "redirect:/Flogin";
		}
	}

	@GetMapping("/ChangePassword")
	public String ShowChangePassword() {
		if (session.getAttribute("loggedInFormer") != null) {
			return "Former/ChangePassword";
		} else {
			return "redirect:/Flogin";
		}
	}

	@PostMapping("/ChangePassword")
	public String ChangePassword(HttpServletRequest request, RedirectAttributes attributes) {
		try {
			Formers formers = (Formers) session.getAttribute("loggedInFormer");
			String newPassword = request.getParameter("newpass");
			String confirmPassword = request.getParameter("confirmpass");
			String oldPassword = request.getParameter("oldpass");

			// Check if new password and confirm password match
			if (!newPassword.equals(confirmPassword)) {
				attributes.addFlashAttribute("error", "New password and Confirm Password do not match.");
				return "redirect:/Former/ChangePassword";
			}

			// Check if current password matches
			if (!formers.getPassword().equals(oldPassword)) {
				attributes.addFlashAttribute("error", "Invalid Old Password.");
				return "redirect:/Former/ChangePassword";
			}

			formers.setPassword(confirmPassword);
			formersRepo.save(formers);
			session.invalidate();
			attributes.addFlashAttribute("msg", "Password Successfully Changed");
			return "redirect:/Flogin";

		} catch (Exception e) {
			attributes.addFlashAttribute("error", "Error :" + e.getMessage());
			return "redirect:/Former/ChangePassword";
		}
	}

	@GetMapping("/Logout")
	public String Logout(RedirectAttributes attributes) {
		if (session.getAttribute("loggedInFormer") == null) {
			attributes.addFlashAttribute("error", "Session Exppired ⚠️");
			return "redirect:/Flogin";
		}

		session.removeAttribute("loggedInFormer");
		attributes.addFlashAttribute("msg", "Logout Successful ✅");
		return "redirect:/Flogin";
	}

	@ModelAttribute
	public void addGlobalAttributes(Model model, HttpSession session) {
		if (session.getAttribute("loggedInFormer") != null) {
			Formers farmer = (Formers) session.getAttribute("loggedInFormer");

			List<Order> pendingOrders = orderRepo.findTop4ByFarmerAndOrderStatusOrderByOrderDateDesc(farmer, "Confirmed");

			model.addAttribute("pendingOrderCount", orderRepo.countByFarmerAndOrderStatus(farmer, "Confirmed"));

			model.addAttribute("pendingOrders", pendingOrders);
		}
	}
}
