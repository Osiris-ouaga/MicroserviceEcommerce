package com.course.client.controllers;

import com.course.client.beans.*;
import com.course.client.proxies.MsCartProxy;
import com.course.client.proxies.MsOrderProxy;
import com.course.client.proxies.MsProductProxy;
import com.course.client.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

// Définition du contrôleur
@Controller
public class ClientController {

    // Injection des dépendances pour les proxies et le service
    @Autowired
    private MsProductProxy msProductProxy;

    @Autowired
    private MsCartProxy msCartProxy;

    @Autowired
    private ClientService clientService;

    @Autowired
    private MsOrderProxy msOrderProxy;

    // Afficher la liste de tous les produits
    @RequestMapping("/all-products")
    public String index(Model model) {
        List<ProductBean> products = msProductProxy.list();
        model.addAttribute("products", products);
        return "products";
    }

    // Afficher la page d'accueil avec la liste des produits
    @RequestMapping("/")
    public String pageHome(Model model) {
        List<ProductBean> products = msProductProxy.list();
        model.addAttribute("products", products);
        return "index";
    }

    // Afficher les détails d'un produit spécifique
    @RequestMapping("/product-detail/{id}")
    public String productDetail(Model model, @PathVariable Long id) {
        Optional<ProductBean> productBean = msProductProxy.get(id);
        List<ProductBean> products = msProductProxy.list();
        if (productBean.isPresent()) {
            model.addAttribute("products", products);
            model.addAttribute("product", productBean.get());
            return "productdetail";
        }
        return "error/404";
    }

    // Ajouter un produit au panier
    @RequestMapping("/add-cart/{productId}")
    public String addProductCart(
            @PathVariable Long productId,
            @RequestParam int quantity,
            Model model) {
        Long newId = 1L;
        Optional<CartBean> cart = msCartProxy.getCart(newId);
        if (cart.isPresent()) {
            CartItemBean cartItemBean = new CartItemBean();
            cartItemBean.setProductId(productId);
            cartItemBean.setQuantity(quantity);
            msCartProxy.addProductToCart(newId,cartItemBean);
            model.addAttribute("cart", cart.get());
            return "redirect:/mon-panier/";
        }
        return "error/404";
    }

    // Supprimer un produit du panier
    @RequestMapping("/remove-cart/{index}")
    public String removeProductCart(
            @PathVariable int index) {
        Long newId = 1L;
        Optional<CartBean> cart = msCartProxy.getCart(newId);
        if (cart.isPresent()) {
            CartBean cartBean = cart.get();
            cartBean.removeProduct(index);
            msCartProxy.updateCart(cartBean);
            return "redirect:/mon-panier/";
        }
        return "error/404";
    }

    // Afficher le contenu du panier
    @RequestMapping("/mon-panier")
    public String myCart(Model model) {
        Long newId = 1L;
        Optional<CartBean> cart = msCartProxy.getCart(newId);
        if (cart.isPresent()) {
            CartBean cartBean = cart.get();
            try {
                List<ProductFinalBean> productFinalBeanList = clientService.CartToProductFinalBean(cartBean.getProducts());
                model.addAttribute("productFinal", productFinalBeanList);
                model.addAttribute("cart", cartBean);
                model.addAttribute("totalPrice", clientService.totalPrice(productFinalBeanList));
                return "cart";
            } catch (Exception e) {
                return "error/404";
            }
        }
        return "error/404";
    }

    // Valider la commande et créer une nouvelle commande
    @RequestMapping("/validation-commande")
    public String createOrder(Model model){
        Long newCartId = 1L;
        Optional<CartBean> optionalCart = msCartProxy.getCart(newCartId);
        int nbrOrder = msOrderProxy.getOrderList().size();
        if (optionalCart.isPresent()) {
            CartBean cartBean = optionalCart.get();
            try {
                OrderBean orderBean = clientService.CartToOrder(cartBean);
                msOrderProxy.createNewOrder(orderBean);
                cartBean.clearCart();
                msCartProxy.updateCart(cartBean);
            } catch (Exception e) {
                return "error/404";
            }
            model.addAttribute("orderNbr", nbrOrder);
            return "order_validate";
        }
        return "error/404";
    }

    // Afficher la liste des commandes de l'utilisateur
    @RequestMapping("/mes-commandes")
    public String myOrders(Model model) {
        List<ProductBean> productList = msProductProxy.list();
        model.addAttribute("productList", productList);
        List<OrderBean> ordersList = msOrderProxy.getOrderList();
        model.addAttribute("ordersList", ordersList);
        return "order";
    }

    // Afficher les détails d'une commande spécifique
    @RequestMapping("/ma-commande/{id}")
    public String myOrder(Model model, @PathVariable int id) {
        List<ProductBean> productList = msProductProxy.list();
        model.addAttribute("productList", productList);
        List<OrderBean> ordersList = msOrderProxy.getOrderList();
        OrderBean order = ordersList.get(id);
        model.addAttribute("order", order);
        model.addAttribute("orderId", id);
        return "orderdetail";
    }
}

