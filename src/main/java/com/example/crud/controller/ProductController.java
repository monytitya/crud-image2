package com.example.crud.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ex")
public class ProductController {

    @Value("${file.upload-dir:localhost}")
    private String uploadDir;

    private final DataSource dataSource;

    public ProductController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // CREATE
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String create(
            @RequestParam("ex_name") String exName,
            @RequestParam("broke_date") String brokeDate,
            @RequestParam("reason") String reason,
            @RequestParam("image") MultipartFile image) {

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imagePath = filePath.toString();

            try (Connection conn = dataSource.getConnection()) {

                String sql = "INSERT INTO memory (ex_name, broke_date, reason, image) VALUES (?, ?, ?, ?)";
                PreparedStatement pr = conn.prepareStatement(sql);

                pr.setString(1, exName);
                pr.setString(2, brokeDate);
                pr.setString(3, reason);
                pr.setString(4, imagePath);

                pr.executeUpdate();

                return "Created Success";
            }

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // GET ALL
    @GetMapping
    public Object getAll() {

        try (Connection conn = dataSource.getConnection()) {

            String sql = "SELECT * FROM memory";
            PreparedStatement pr = conn.prepareStatement(sql);
            ResultSet rs = pr.executeQuery();

            List<Map<String, Object>> list = new ArrayList<>();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();

                row.put("id", rs.getInt("id"));
                row.put("ex_name", rs.getString("ex_name"));
                row.put("broke_date", rs.getString("broke_date"));
                row.put("reason", rs.getString("reason"));

                String img = rs.getString("image");
                if (img != null) {
                    img = img.replace("\\", "/");
                    img = "http://localhost:8081/uploads/" + Paths.get(img).getFileName();
                }

                row.put("image", img);

                list.add(row);
            }

            return list;

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String update(
            @PathVariable int id,
            @RequestParam String ex_name,
            @RequestParam String broke_date,
            @RequestParam String reason,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try (Connection conn = dataSource.getConnection()) {

            String imagePath = null;

            if (image != null && !image.isEmpty()) {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                imagePath = filePath.toString();
            }

            String sql = (imagePath != null)
                    ? "UPDATE memory SET ex_name=?, broke_date=?, reason=?, image=? WHERE id=?"
                    : "UPDATE memory SET ex_name=?, broke_date=?, reason=? WHERE id=?";

            PreparedStatement pr = conn.prepareStatement(sql);

            pr.setString(1, ex_name);
            pr.setDate(2, java.sql.Date.valueOf(broke_date)); // if DB column is DATE
            pr.setString(3, reason);

            if (imagePath != null) {
                pr.setString(4, imagePath);
                pr.setInt(5, id);
            } else {
                pr.setInt(4, id);
            }

            int updated = pr.executeUpdate();
            return updated > 0 ? "Update Success" : "No record found";

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String delete(@PathVariable int id) {

        try (Connection conn = dataSource.getConnection()) {

            String sql = "DELETE FROM memory WHERE id=?";
            PreparedStatement pr = conn.prepareStatement(sql);
            pr.setInt(1, id);
            pr.executeUpdate();

            return "Delete Success";

        } catch (Exception e) {
            return e.getMessage();
        }
    }
}