package com.luv2code.springbootlibrary.entity;


import lombok.Data;
import net.bytebuddy.dynamic.loading.InjectionClassLoader;

import javax.persistence.*;

@Entity
@Table(name = "checkout")
@Data
public class Checkout {

    public Checkout(){}

    public  Checkout(String userEmail, String checkoutData, String returnData, Long bookId){
        this.userEmail = userEmail;
        this.checkoutData = checkoutData;
        this.returnData = returnData;
        this.bookId = bookId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "checkout_data")
    private  String checkoutData;

    @Column(name = "return_data")
    private String returnData;

    @Column(name = "book_id")
    private Long bookId;
}
