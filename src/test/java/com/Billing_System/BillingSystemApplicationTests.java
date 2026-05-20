package com.Billing_System;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class BillingSystemApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		System.out.println("=== PRINTING ALL ROWS OF BIN_LOCATIONS ===");
		try {
			List<Map<String, Object>> bins = jdbcTemplate.queryForList(
				"SELECT id, zone, rack, level_number, bin_code, bin_full_code, capacity_units, current_units, current_product_id, is_active " +
				"FROM bin_locations"
			);
			for (Map<String, Object> bin : bins) {
				System.out.println(String.format("Bin: %s | FullCode: %s | Zone: %s | Rack: %s | Level: %s | Code: %s | Cap: %s | Cur: %s | Prod: %s | Active: %s",
					bin.get("id"),
					bin.get("bin_full_code"),
					bin.get("zone"),
					bin.get("rack"),
					bin.get("level_number"),
					bin.get("bin_code"),
					bin.get("capacity_units"),
					bin.get("current_units"),
					bin.get("current_product_id"),
					bin.get("is_active")
				));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("=========================================");
	}
}
