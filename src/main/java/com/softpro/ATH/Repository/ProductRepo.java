package com.softpro.ATH.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.softpro.ATH.Model.Formers;
import com.softpro.ATH.Model.Product;

public interface ProductRepo extends JpaRepository<Product, Long>{

	List<Product> findByFarmer(Formers formers);
	List<Product> findByFarmerAndStatus(Formers farmer, String status);
	List<Product> findByStatus(String string);
	
	Product findByProductNameAndFarmer(String productName, Formers farmer);
	Object countByFarmer(Formers farmer);
	//List<Product> findByCategory(String categoryName);
	List<Product> findByCategoryAndStatus(String categoryName, String string);
	
	@Query("SELECT SUM(p.pricePerUnit * p.quantity) FROM Product p WHERE p.farmer.id = :farmerId AND p.status = 'Available'")
	BigDecimal calculateInStockRevenue(@Param("farmerId") Long farmerId);

}
