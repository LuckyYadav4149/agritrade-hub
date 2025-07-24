package com.softpro.ATH.Dto;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import com.softpro.ATH.Model.Formers;

public class ProductDto {
	
	private Long id;

	private String productName;

	private int quantity;

	private BigDecimal pricePerUnit;

	private String category;

	private Formers farmer; // Assuming you renamed Formers to Farmer

	private String status; // e.g., Available, OutOfStock, SoldOut

	private MultipartFile image;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getPricePerUnit() {
		return pricePerUnit;
	}

	public void setPricePerUnit(BigDecimal pricePerUnit) {
		this.pricePerUnit = pricePerUnit;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Formers getFarmer() {
		return farmer;
	}

	public void setFarmer(Formers farmer) {
		this.farmer = farmer;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public MultipartFile getImage() {
		return image;
	}

	public void setImage(MultipartFile image) {
		this.image = image;
	}

}
