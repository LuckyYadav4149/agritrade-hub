package com.softpro.ATH.Controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.softpro.ATH.Dto.EnquiryDto;
import com.softpro.ATH.Dto.FormersDto;
import com.softpro.ATH.Dto.MerchantDto;
import com.softpro.ATH.Model.AdminInfo;
import com.softpro.ATH.Model.Enquiry;
import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Merchant;
import com.softpro.ATH.Repository.AdminInfoRepo;
import com.softpro.ATH.Repository.EnquiryRepo;
import com.softpro.ATH.Repository.FormersRepo;
import com.softpro.ATH.Repository.MerchantRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class MainController {
	
	@Autowired
	EnquiryRepo eqRepo;
	
	@Autowired
	AdminInfoRepo adRepo;
	
	@Autowired
	FormersRepo formersRepo;
	
	@Autowired
	MerchantRepo merchantRepo;
	
	//Show Index Page	
	@GetMapping({"/", "/home"})
	public String ShowIndex(Model model)
	{
		model.addAttribute("active1", "active");
		return "index";
	}
	
	@GetMapping({"/aboutus"})
	public String ShowAbout()
	{
		return "aboutus";
	}
	
	@GetMapping({"/services"})
	public String ShowServices()
	{
		return "services";
	}
	
	
	
	//To Show The Admin Login Page
	@GetMapping("/adminlogin")
	public String ShowAdminLogin()
	{
		return "adminlogin";
	}
	
	@PostMapping("/adminlogin")
	public String AdminLogin(HttpServletRequest request, HttpSession session, RedirectAttributes attributes)
	{
		try {
			
			String userid = request.getParameter("userid");
			String password = request.getParameter("password");
			
			AdminInfo adinfo = adRepo.findById(userid).get();
			if (adinfo.getPassword().equals(password)) {
				session.setAttribute("admin", adinfo.getUserid());
				return "redirect:/admin/dashboard";
			}
			else {
				attributes.addFlashAttribute("msg", "Invalid UserId or Password ⚠️");
			}
			return "redirect:/adminlogin";
			
		} catch (Exception e) {
			attributes.addFlashAttribute("msg", "User Does not Exists ❌");
			return "redirect:/adminlogin";
		}
	}
	
	//To Show The Former Registration Page
	@GetMapping("/fregistration")
	public String ShowFormerRegistration(Model model)
	{
		model.addAttribute("active4", "active");
		FormersDto dto = new FormersDto();
		model.addAttribute("dto", dto);
		return "formerregistration";
	}
	
	@PostMapping("/fregistration")
	public String FormerRegistration(@ModelAttribute FormersDto dto,RedirectAttributes attributes)
	{
		try {
			
			if (formersRepo.existsByEmail(dto.getEmail())) {
				
				attributes.addFlashAttribute("errormsg", "User Already Exists ⚠️");
				return "redirect:/fregistration";
			}
			
			Formers former = new Formers();
			former.setName(dto.getName());
			former.setEmail(dto.getEmail());
			former.setAadharno(dto.getAadharno());
			former.setContactno(dto.getContactno());
			former.setPassword(dto.getPassword());
			former.setAddress(dto.getAddress());
			
			Date dt = new Date();
			SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
			String regdate = df.format(dt);
			former.setRegdate(regdate);
			former.setStatus("Pending");
			formersRepo.save(former);
			attributes.addFlashAttribute("successmsg", "Registration Successful ✅");
			return "redirect:/fregistration";
		} catch (Exception e) {
			attributes.addFlashAttribute("errormsg", "Error : "+e.getMessage());
			return "redirect:/fregistration";
		}
	}
	
	
	@GetMapping("/Flogin")
	public String ShowFormerLogin(Model model)
	{
		model.addAttribute("dto", new FormersDto());
		return "formerlogin";
	}
	
	@PostMapping("/Flogin")
	public String FormerLogin(@ModelAttribute FormersDto dto, RedirectAttributes attributes, HttpSession session)
	{
		try {
			
			Formers formers = formersRepo.findFormerByEmail(dto.getEmail());
			
			if (formers.getPassword().equals(dto.getPassword())) {
				if(formers.getStatus().equalsIgnoreCase("Verified"))
				{
					session.setAttribute("loggedInFormer", formers);
					return "redirect:/Former/Dashboard";
				}
				else if(formers.getStatus().equalsIgnoreCase("Pending")){
					attributes.addFlashAttribute("error", "Not Approved, Please wait for Admin Approval");
					return "redirect:/Flogin";
				}
				else {
					attributes.addFlashAttribute("error", "Your Login is Disabled, Please Contact Administrator!");
					return "redirect:/Flogin";
				}
			}
			else {
				attributes.addFlashAttribute("error", "Invalid Password");
				return "redirect:/Flogin";
			}
		
		} catch (Exception e) {
			attributes.addFlashAttribute("error", "User does not Exists! ❌");
			return "redirect:/Flogin";
		}
	}
	
	//To Show The Merchant Registration Page
	@GetMapping("/mregistration")
	public String ShowMerchantRegistration(Model model)
	{
		MerchantDto dto = new MerchantDto();
		model.addAttribute("dto", dto);
		return "merchantregistration";
	}
	
	@PostMapping("/mregistration")
	public String MerchantRegistration(@ModelAttribute MerchantDto dto, RedirectAttributes attributes)
	{
		try {
			
			if (merchantRepo.existsByEmail(dto.getEmail())) {
				attributes.addFlashAttribute("errormsg", "User Already Exists ⚠️");
				return "redirect:/mregistration";
			}
			
			Merchant merchant = new Merchant();
			merchant.setName(dto.getName());
			merchant.setEmail(dto.getEmail());
			merchant.setContactno(dto.getContactno());
			merchant.setPassword(dto.getPassword());
			merchant.setAadharno(dto.getAadharno());
			merchant.setPancardno(dto.getPancardno());
			merchant.setAddress(dto.getAddress());
			merchant.setStatus("Pending");
			Date dt = new Date();
			SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
			String regdate = df.format(dt);
			merchant.setRegdate(regdate);
			merchantRepo.save(merchant);
			attributes.addFlashAttribute("successmsg", "Registration Successfull ✅");
			return "redirect:/mregistration";
			
		} catch (Exception e) {
			attributes.addFlashAttribute("errormsg", "Error : "+e.getMessage());
			return "redirect:/mregistration";
		}
	}
	
	@GetMapping("/Mlogin")
	public String ShowMerchantLogin(Model model)
	{
		model.addAttribute("dto", new MerchantDto());
		return "merchantlogin";
	}
	
	@PostMapping("/Mlogin")
	public String MerchantLogin(@ModelAttribute MerchantDto dto, RedirectAttributes attributes, HttpSession session)
	{
		try {
			
			Merchant merchant = merchantRepo.findByEmail(dto.getEmail());
			
			if (merchant.getPassword().equals(dto.getPassword())) {
				if(merchant.getStatus().equalsIgnoreCase("Verified"))
				{
					session.setAttribute("loggedInMerchant", merchant);
					return "redirect:/Merchant/Dashboard";
				}
				else if(merchant.getStatus().equalsIgnoreCase("Pending")){
					attributes.addFlashAttribute("error", "Not Approved, Please wait for Admin Approval");
					return "redirect:/Mlogin";
				}
				else {
					attributes.addFlashAttribute("error", "Your Login is Disabled, Please Contact Administrator!");
					return "redirect:/Mlogin";
				}
			}
			else {
				attributes.addFlashAttribute("error", "Invalid Password");
				return "redirect:/Mlogin";
			}
		
		} catch (Exception e) {
			attributes.addFlashAttribute("error", "User does not Exists! ❌");
			return "redirect:/Mlogin";
		}
	}
	
	//To Show The Contact Us Page
	@GetMapping("/contactus")
	public String ShowContactUs(Model model)
	{
		EnquiryDto dto = new EnquiryDto();
		model.addAttribute("dto", dto);
		model.addAttribute("active5", "active");
		return "contactus";
	}
	
	//To Submit The Contact Us Form PostMapping
	@PostMapping("/contactus")
	@ResponseBody
	public String ContactUs(@ModelAttribute EnquiryDto dto) {
	    try {
	        Enquiry enquiry = new Enquiry();
	        enquiry.setName(dto.getName());
	        enquiry.setContactno(dto.getContactno());
	        enquiry.setEmail(dto.getEmail());
	        enquiry.setAddress(dto.getAddress());
	        enquiry.setMessage(dto.getMessage());
	        Date dt = new Date();
	        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
	        String eqdate = df.format(dt);
	        enquiry.setEnquirydate(eqdate);
	        eqRepo.save(enquiry);
	        return "success";  // <-- Just return a clear keyword
	    } catch (Exception e) {
	        return "error -> : " + e.getMessage();
	    }
	}

}
