package com.luv2code.springbootlibrary.service;


import com.luv2code.springbootlibrary.dao.BookRepository;
import com.luv2code.springbootlibrary.dao.CheckoutRepository;
import com.luv2code.springbootlibrary.dao.HistoryRepository;
import com.luv2code.springbootlibrary.dao.PaymentRepository;
import com.luv2code.springbootlibrary.entity.Book;
import com.luv2code.springbootlibrary.entity.Checkout;
import com.luv2code.springbootlibrary.entity.History;
import com.luv2code.springbootlibrary.entity.Payment;
import com.luv2code.springbootlibrary.responsemodels.ShelfCurrentLoansResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class BookService {

    private BookRepository bookRepository;

    private CheckoutRepository checkoutRepository;

    private HistoryRepository historyRepository;

    private PaymentRepository paymentRepository;

    public BookService(BookRepository bookRepository, CheckoutRepository checkoutRepository, HistoryRepository historyRepository,
                       PaymentRepository paymentRepository) {
        this.bookRepository = bookRepository;
        this.checkoutRepository = checkoutRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
    }

    public Book checkoutBook (String userEmail, Long bookId) throws Exception {

        Optional<Book> book = bookRepository.findById(bookId);

        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);

        // only want a user to be able to check out a single book one time
        if (!book.isPresent() || validateCheckout != null || book.get().getCopiesAvailable() <= 0){
            throw new Exception("Book doesn't exist or already checked out by user");
        }

        //if that are late that need to be returned and need to be paid for user can check out again.
        List<Checkout> currentBooksCheckedOut = checkoutRepository.findBookByUserEmail(userEmail);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        boolean bookNeedReturned = false;

        for (Checkout checkout: currentBooksCheckedOut) {
            Date d1 = sdf.parse(checkout.getReturnDate());
            Date d2 = sdf.parse(LocalDate.now().toString());

            TimeUnit time = TimeUnit.DAYS;

            double differenceTime = time.convert(d1.getTime() - d2.getTime(), TimeUnit.MILLISECONDS);

            if (differenceTime < 0) {
                bookNeedReturned = true;
                break;
            }
        }

        Payment userPayment = paymentRepository.findByUserEmail(userEmail);
        if ((userPayment != null && userPayment.getAmount() > 0) || (userPayment != null && bookNeedReturned)) {
            throw new Exception("Outstanding fees");
        }

        if (userPayment == null) {
            Payment payment = new Payment();
            payment.setAmount(00.00);
            payment.setUserEmail(userEmail);
            paymentRepository.save(payment);
        }



        book.get().setCopiesAvailable(book.get().getCopiesAvailable() - 1);
        bookRepository.save(book.get());

        Checkout checkout = new Checkout(
                userEmail,
                LocalDate.now().toString(),
                LocalDate.now().plusDays(7).toString(),
                book.get().getId()
        );

        checkoutRepository.save(checkout);

        return book.get();
    }

    //verifying if this book is checked out by the user or not
    public Boolean checkoutBookByUser(String userEmail, Long bookId){
       Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);
       if (validateCheckout != null){
           return true;
       }else {
           return false;
       }
    }

    //how many element are in this list
    public int currentLoansCount(String userEmail) {
        return checkoutRepository.findBookByUserEmail(userEmail).size();
    }


    // section for ShelfCurrentLoansResponse
    public List<ShelfCurrentLoansResponse> currentLoans(String userEmail) throws Exception {

        List<ShelfCurrentLoansResponse> shelfCurrentLoansResponses = new ArrayList<>();

        List<Checkout> checkoutList = checkoutRepository.findBookByUserEmail(userEmail);
        List<Long> bookIdList = new ArrayList<>();

        for (Checkout i: checkoutList) {
            bookIdList.add(i.getBookId());
        }

        List<Book> books = bookRepository.findBookByBookIds(bookIdList);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        for (Book book : books) {
            Optional<Checkout> checkout = checkoutList.stream()
                    .filter(x -> x.getBookId() == book.getId()).findFirst();

            if (checkout.isPresent()) {

                Date d1 = sdf.parse(checkout.get().getReturnDate());
                Date d2 = sdf.parse(LocalDate.now().toString());

                TimeUnit time = TimeUnit.DAYS;

                long difference_In_Time = time.convert(d1.getTime() - d2.getTime(),
                        TimeUnit.MILLISECONDS);

                shelfCurrentLoansResponses.add(new ShelfCurrentLoansResponse(book, (int) difference_In_Time));
            }
        }
        return shelfCurrentLoansResponses;
    }

    //return Book
    public void returnBook (String userEmail, Long bookId) throws Exception {
        Optional<Book> book = bookRepository.findById(bookId);

        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);

        if (!book.isPresent() || validateCheckout == null) {
            throw new Exception("Book does not exist or not checked out by user");
        }

        book.get().setCopiesAvailable(book.get().getCopiesAvailable() + 1);

        bookRepository.save(book.get());

        //for payment if he is late
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date d1 = sdf.parse(validateCheckout.getReturnDate());
        Date d2 = sdf.parse(LocalDate.now().toString());

        TimeUnit time = TimeUnit.DAYS;

        double differenceTime = time.convert(d1.getTime() - d2.getTime(), TimeUnit.MILLISECONDS);
        if (differenceTime < 0) {
            Payment payment = paymentRepository.findByUserEmail(userEmail);

            payment.setAmount(payment.getAmount() + (differenceTime * -1));
            paymentRepository.save(payment);
        }




        checkoutRepository.deleteById(validateCheckout.getId());

        // Save History
        History history = new History(
                userEmail,
                validateCheckout.getCheckoutDate(),
                LocalDate.now().toString(),
                book.get().getTitle(),
                book.get().getAuthor(),
                book.get().getDescription(),
                book.get().getImg()
        );

        historyRepository.save(history);
    }


    //renew loan
    public void renewLoan(String userEmail, Long bookId) throws Exception {
        Checkout validateCheckout = checkoutRepository.findByUserEmailAndBookId(userEmail, bookId);

        if(validateCheckout == null){
            throw new Exception("Book does not exist or not check out by user");
        }

        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date d1 = sdFormat.parse(validateCheckout.getReturnDate());
        Date d2 = sdFormat.parse(LocalDate.now().toString());

        if (d1.compareTo(d2) > 0 || d1.compareTo(d2) == 0) {
            validateCheckout.setCheckoutDate((LocalDate.now().plusDays(7).toString()));
            checkoutRepository.save(validateCheckout);
        }
    }



}
