package com.sip.wbx;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.sip.wbx.entities.*;
import com.sip.wbx.repositories.*;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final CategorieRepository categorieRepo;
    private final ProduitRepository produitRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(RoleRepository roleRepo, CategorieRepository categorieRepo,
            ProduitRepository produitRepo, UserRepository userRepo,
            PasswordEncoder encoder) {
this.roleRepo = roleRepo;
this.categorieRepo = categorieRepo;
this.produitRepo = produitRepo;
this.userRepo = userRepo;
this.encoder = encoder;
}

    @Override
    public void run(String... args) {
        if (roleRepo.count() == 0) {
            roleRepo.save(new Role(null, "ADMIN"));
            roleRepo.save(new Role(null, "CLIENT"));
        }

        // Utilisateur de test (temporaire : le mot de passe sera encodé à l'étape Security)
     // Encode any plain-text password left over from the tests (BCrypt hashes start with "$2")
        userRepo.findAll().forEach(u -> {
            if (!u.getPassword().startsWith("$2")) {
                u.setPassword(encoder.encode(u.getPassword()));
                userRepo.save(u);
            }
        });

        if (userRepo.findByEmail("client@wbx.tn").isEmpty()) {
            User c = new User();
            c.setEmail("client@wbx.tn");
            c.setPassword(encoder.encode("changeme"));
            c.setNom("Demo");
            c.setPrenom("Client");
            c.setEnabled(true);
            roleRepo.findByLibelle("CLIENT").ifPresent(r -> c.getRoles().add(r));
            userRepo.save(c);
        }

        if (userRepo.findByEmail("admin@wbx.tn").isEmpty()) {
            User a = new User();
            a.setEmail("admin@wbx.tn");
            a.setPassword(encoder.encode("Admin@123"));    // development only: change it later
            a.setNom("Admin");
            a.setPrenom("WarrantyBox");
            a.setEnabled(true);
            roleRepo.findByLibelle("ADMIN").ifPresent(r -> a.getRoles().add(r));
            userRepo.save(a);
        }

        if (produitRepo.count() > 0) return;   // catalogue chargé seulement s'il n'y a aucun produit

        Categorie smart = cat("Smartphones", "Téléphones mobiles");
        Categorie pc    = cat("Ordinateurs", "PC portables");
        Categorie audio = cat("Audio", "Enceintes et casques");
        Categorie elec  = cat("Électroménager", "Petit électroménager de cuisine");
        Categorie tv    = cat("Téléviseurs", "Smart TV");

        // Prix relevés sur Tunisianet / Spacenet / Primini le 10/11/2026
        prod("Galaxy S25 5G 12/256 Go", "Samsung", "S25", 3399, "Smartphone Samsung Galaxy S25 5G, 12 Go RAM, 256 Go", 24, smart);
        prod("Galaxy S25 FE 5G 8/256 Go", "Samsung", "S25 FE", 2799, "Smartphone Samsung Galaxy S25 FE 5G, 8 Go RAM, 256 Go", 24, smart);

        prod("IdeaPad Slim 3 15IRU8", "Lenovo", "82X700DFFG", 1729, "PC portable Core i3-1315U, 8 Go, 512 Go SSD", 24, pc);
        prod("IdeaPad Slim 3 15IRH10 i5", "Lenovo", "83K100E6FG", 2459, "PC portable Core i5-13420H, 16 Go DDR5, 512 Go SSD", 24, pc);
        prod("IdeaPad Slim 3 15IRH10 i7", "Lenovo", "83K100E7FE", 2399, "PC portable Core i7-13620H, 16 Go DDR5, 512 Go SSD", 24, pc);

        prod("Flip 6", "JBL", "FLIP6", 485, "Enceinte Bluetooth portable étanche, 12 h d'autonomie", 12, audio);

        prod("Easy Fry Essential", "Moulinex", "EZ130810", 329, "Friteuse à air chaud sans huile", 24, elec);
        prod("Air Fryer 3,8 L", "Kenwood", "HFP30.000 BK", 299, "Friteuse à air chaud 3,8 litres", 24, elec);

        prod("TV 55\" Crystal UHD U8000H", "Samsung", "UA55U8000H", 1819, "Smart TV 55 pouces 4K Crystal UHD", 24, tv);
        prod("TV 55\" Crystal UHD CU7000", "Samsung", "UA55CU7000", 1839, "Smart TV 55 pouces 4K Crystal UHD", 24, tv);
    }

    // Réutilise la catégorie si elle existe déjà (ex. "Smartphones" créée en test)
    private Categorie cat(String libelle, String description) {
        return categorieRepo.findAll().stream()
            .filter(c -> c.getLibelle().equalsIgnoreCase(libelle))
            .findFirst()
            .orElseGet(() -> {
                Categorie c = new Categorie();
                c.setLibelle(libelle);
                c.setDescription(description);
                return categorieRepo.save(c);
            });
    }

    private void prod(String nom, String marque, String modele, double prix,
                      String description, int garantieMois, Categorie c) {
        Produit p = new Produit();
        p.setNom(nom);
        p.setMarque(marque);
        p.setModele(modele);
        p.setPrix(prix);
        p.setDescription(description);
        p.setDureeGarantieStandard(garantieMois);
        p.setCategorie(c);
        produitRepo.save(p);
    }
}