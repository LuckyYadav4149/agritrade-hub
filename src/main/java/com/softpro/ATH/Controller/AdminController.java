package com.softpro.ATH.Controller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.API.SendAutoEmail;
import com.softpro.ATH.Model.AdminInfo;
import com.softpro.ATH.Model.Category;
import com.softpro.ATH.Model.Enquiry;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Model.Order;
import com.softpro.ATH.Model.Product;
import com.softpro.ATH.Repository.AdminInfoRepo;
import com.softpro.ATH.Repository.CategoryRepo;
import com.softpro.ATH.Repository.EnquiryRepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.MerchantRepo;
import com.softpro.ATH.Repository.OrderRepo;
import com.softpro.ATH.Repository.ProductRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
	
	@Autowired
	AdminInfoRepo adRepo;
	
	@Autowired
	EnquiryRepo eqRepo;
	
	@Autowired
	FormersRepo formersRepo;
	
	@Autowired
	MerchantRepo merchantRepo;
	
	@Autowired
	OrderRepo orderRepo;
	
	@Autowired
	ProductRepo productRepo;
	
	@Autowired
	CategoryRepo categoryRepo;
	
	@Autowired
	private SendAutoEmail sendAutoEmail;
	
	@GetMapping("/dashboard")
	public String ShowDashboard(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active1", "active");
		    
		    
		    model.addAttribute("totalFarmers", formersRepo.count());
		    model.addAttribute("totalMerchants", merchantRepo.count());
		    model.addAttribute("totalProducts", productRepo.count());
		    model.addAttribute("totalOrders", orderRepo.count());
		    model.addAttribute("completedOrders", orderRepo.countByOrderStatus("Delivered"));
		    model.addAttribute("cancelledOrders", orderRepo.countByOrderStatus("Cancelled"));
		    model.addAttribute("newEnquiries", eqRepo.count());
			return "admin/dashboard";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/EditProfile")
	public String ShowEditProfile(Model model, HttpSession session)
	{
		if (session.getAttribute("admin")==null) {
			return "redirect:/adminlogin";
		}
		AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
	    model.addAttribute("admininfo", admininfo);
		return "admin/EditProfile";
	}
	
	@PostMapping("/UpdateProfile")
	public String UpdateProfilePic(@RequestParam("file") MultipartFile file, RedirectAttributes attributes, HttpSession session) {
	    try {
	        String storageFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
	        String uploadDir = "public/ProfilePic/";
	        Path uploadPath = Paths.get(uploadDir);

	        if (!Files.exists(uploadPath)) {
	            Files.createDirectories(uploadPath);
	        }

	        // Get current admin info
	        AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();

	        // Delete previous profile picture if it exists
	        String oldProfilePic = admininfo.getProfilepic();
	        if (oldProfilePic != null && !oldProfilePic.isEmpty()) {
	            Path oldFilePath = Paths.get(uploadDir + oldProfilePic);
	            if (Files.exists(oldFilePath)) {
	                Files.delete(oldFilePath);
	            }
	        }

	        // Save the new profile picture
	        try (InputStream inputStream = file.getInputStream()) {
	            Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
	        }

	        // Update DB
	        admininfo.setProfilepic(storageFileName);
	        adRepo.save(admininfo);

	        return "redirect:/admin/EditProfile";

	    } catch (Exception e) {
	        attributes.addFlashAttribute("msg", "Error : " + e.getMessage());
	        return "redirect:/admin/EditProfile";
	    }
	}

	
	@GetMapping("/newformer")
	public String NewFormer(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active3", "active");
		    
		    List<Formers> FList = formersRepo.findAllByStatus("Pending");
		    model.addAttribute("FList", FList.reversed());
			return "admin/newformer";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/verifiedformer")
	public String VerifiedFormer(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active3", "active");
		    
		    List<Formers> FList = formersRepo.findAllByStatus("Verified");
		    model.addAttribute("FList", FList.reversed());
			return "admin/verifiedformer";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/disableformer")
	public String DisabledFormer(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active3", "active");
		    
		    List<Formers> FList = formersRepo.findAllByStatus("Disabled");
		    model.addAttribute("FList", FList.reversed());
			return "admin/disableformer";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/formerstatus")
	public String FormerStatusUpdate(@RequestParam("id") long id)
	{
		try {
			Formers former = formersRepo.findById(id).get();
			List<Product> products = productRepo.findByFarmer(former);
			if(former.getStatus().equals("Pending"))
			{
				former.setStatus("Verified");
				formersRepo.save(former);
				sendAutoEmail.sendApprovalEmail(former);
				return "redirect:/admin/newformer";
			}
			else if (former.getStatus().equals("Verified")) {
				former.setStatus("Disabled");
				formersRepo.save(former);
				for(Product product:products)
				{
					if (product.getQuantity()>0) {
						product.setStatus("OutOfStock");
					}
				}
				productRepo.saveAll(products);
				return "redirect:/admin/verifiedformer";
			}
			else{
				former.setStatus("Verified");
				formersRepo.save(former);
				if (products.size()>0) {
					for(Product product : products)
					{
						if (product.getQuantity()>0) {
							product.setStatus("Available");
						}
					}
				}
				productRepo.saveAll(products);
				return "redirect:/admin/disableformer";
			}
			
			
		} catch (Exception e) {
			System.err.println("Error : "+e.getMessage());
			return "redirect:/newformer";
		}
	}
	
	@GetMapping("/newmerchant")
	public String NewMerchant(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active2", "active");
		    
		    List<Merchant> MList = merchantRepo.findAllByStatus("Pending");
		    model.addAttribute("MList", MList.reversed());
			return "admin/newmerchant";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/verifiedmerchant")
	public String VerifiedMerchant(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active2", "active");
		    
		    List<Merchant> MList = merchantRepo.findAllByStatus("Verified");
		    model.addAttribute("MList", MList.reversed());
			return "admin/verifiedmerchant";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/disablemerchant")
	public String DisabledMerchant(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active2", "active");
		    
		    List<Merchant> MList = merchantRepo.findAllByStatus("Disabled");
		    model.addAttribute("MList", MList.reversed());
			return "admin/disablemerchant";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/merchantstatus")
	public String MerchantStatusUpdate(@RequestParam("id") long id)
	{
		try {
			Merchant merchant = merchantRepo.findById(id).get();
			if(merchant.getStatus().equals("Pending"))
			{
				merchant.setStatus("Verified");
				merchantRepo.save(merchant);
				sendAutoEmail.sendApprovalEmail(merchant);
				return "redirect:/admin/newmerchant";
			}
			else if (merchant.getStatus().equals("Verified")) {
				merchant.setStatus("Disabled");
				merchantRepo.save(merchant);
				return "redirect:/admin/verifiedmerchant";
			}
			else{
				merchant.setStatus("Verified");
				merchantRepo.save(merchant);
				return "redirect:/admin/disablemerchant";
			}
			
			
		} catch (Exception e) {
			System.err.println("Error : "+e.getMessage());
			return "redirect:/newmerchant";
		}
	}
	
	/*
	 * @GetMapping("/deletemerchant")
	 * 
	 * @ResponseBody public String DeleteMerchant(@RequestParam("id") long id,
	 * HttpServletRequest request) { Merchant merchant =
	 * merchantRepo.findById(id).get(); merchantRepo.delete(merchant);
	 * System.err.println(merchant.getName()+" is deleted successfully"); String
	 * referer = request.getHeader("Referer"); return
	 * "redirect:/admin/"+(referer!=null ? referer : "/dashboard"); }
	 */
	
	@GetMapping("/ViewOrder")
	public String ShowViewOrders(Model model, HttpSession session)
	{
		if (session.getAttribute("admin")==null) {
			return "redirect:/adminlogin";
		}
		AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
	    model.addAttribute("admininfo", admininfo);
	    
		List<Order> orderList = orderRepo.findAll();
		model.addAttribute("orderList", orderList.reversed());
		return "admin/vieworders";
	}
	
	@GetMapping("/AddCategory")
	public String ShowAddCategory(Model model, HttpSession session)
	{
		if (session.getAttribute("admin")==null) {
			return "redirect:/adminlogin";
		}
		AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
	    model.addAttribute("admininfo", admininfo);
		model.addAttribute("active5", "active");
		return "admin/addcategory";
	}
	
	@PostMapping("/AddCategory")
	public String AddCategory(@RequestParam("category") String cate, RedirectAttributes attributes)
	{
		try {
		
			Category category = new Category();
			if(categoryRepo.existsByCategory(cate)) 
			{
				attributes.addFlashAttribute("msg", "This category Already Exists!");
				return "redirect:/admin/AddCategory";
			}
			category.setCategory(cate);
			categoryRepo.save(category);
			attributes.addFlashAttribute("msg", "Category Successfully Added");
			return "redirect:/admin/AddCategory";
		} catch (Exception e) {
			attributes.addFlashAttribute("msg", "Error : "+e.getMessage());
			return "redirect:/admin/AddCategory";
		}
	}
		
	@GetMapping("/enquiry")
	public String ShowEnquiry(HttpSession session, Model model, RedirectAttributes attributes)
	{
	    if (session.getAttribute("admin")!=null) {
	    	AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
		    model.addAttribute("active6", "active");
		    
		    List<Enquiry> enquiryList = eqRepo.findAll();
		    model.addAttribute("enquiryList", enquiryList);
			return "admin/enquiry";
		}
	    else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@GetMapping("/enquirydelete")
	public String DeleteEnquiry(@RequestParam("id") long id, RedirectAttributes attributes)
	{
		try {
			
			Enquiry enquiry = eqRepo.findById(id).get();
			eqRepo.delete(enquiry);
			System.err.println("Successfully Deleted");
			
		} catch (Exception e) {
			System.err.println("Error : "+e.getMessage());
		}
		return "redirect:/admin/enquiry";
	}
	
	@GetMapping("/ChangePassword")
	public String ShowChangePassword(HttpSession session, RedirectAttributes attributes, Model model)
	{
		if(session.getAttribute("admin")!=null)
		{	
			AdminInfo admininfo = adRepo.findById(session.getAttribute("admin").toString()).get();
		    model.addAttribute("admininfo", admininfo);
			return "admin/changepassword";
		}
		else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
	
	@PostMapping("/ChangePassword")
	public String ChangePassword(HttpServletRequest request, RedirectAttributes attributes, HttpSession session)
	{
		try {
			AdminInfo adInfo = adRepo.findById(session.getAttribute("admin").toString()).get();
	        String newPassword = request.getParameter("newpass");
	        String confirmPassword = request.getParameter("confirmpass");
	        String oldPassword = request.getParameter("oldpass");
	        
	        // Check if new password and confirm password match
	        if (!newPassword.equals(confirmPassword)) {
	            attributes.addFlashAttribute("error", "New password and Confirm Password do not match.");
	            return "redirect:/admin/ChangePassword";
	        }
	        
	        // Check if current password matches
	        if (!adInfo.getPassword().equals(oldPassword)) {
	            attributes.addFlashAttribute("error", "Invalid Old Password.");
	            return "redirect:/admin/ChangePassword";
	        }
	        
	        adInfo.setPassword(confirmPassword);
	        adRepo.save(adInfo);
	        session.invalidate();
	        attributes.addFlashAttribute("msg", "Password Successfully Changed");
	        return "redirect:/adminlogin";
			
		} catch (Exception e) {
			attributes.addFlashAttribute("error", "Error :"+e.getMessage());
			return "redirect:/admin/ChangePassword";
		}
	}
	
	@GetMapping("/logout")
	public String Logout(HttpSession session, RedirectAttributes attributes)
	{
		if(session.getAttribute("admin")!=null)
		{
			session.removeAttribute("admin");
			attributes.addFlashAttribute("msg", "Successfully Logout ✅");
			return "redirect:/adminlogin";
		}
		else {
	    	attributes.addFlashAttribute("msg", "Session Expired 👾");
			return "redirect:/adminlogin";
		}
	}
}
