
// We need to import the java.sql package to use JDBC
import java.sql.*;

// for reading from the command line
import java.io.*;

// for the login window
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


/*
 * This class implements a graphical login window and a simple text
 * interface for interacting with the branch table 
 */ 
public class itemtable implements ActionListener
{
    // command line reader 
    private BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private Connection con;

    // user is allowed 3 login attempts
    private int loginAttempts = 0;

    // components of the login window
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JFrame mainFrame;


    /*
     * constructs login window and loads JDBC driver
     */ 
    public itemtable()
    {
      mainFrame = new JFrame("User Login");

      JLabel usernameLabel = new JLabel("Enter username: ");
      JLabel passwordLabel = new JLabel("Enter password: ");

      usernameField = new JTextField(10);
      passwordField = new JPasswordField(10);
      passwordField.setEchoChar('*');

      JButton loginButton = new JButton("Log In");

      JPanel contentPane = new JPanel();
      mainFrame.setContentPane(contentPane);


      // layout components using the GridBag layout manager

      GridBagLayout gb = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();

      contentPane.setLayout(gb);
      contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

      // place the username label 
      c.gridwidth = GridBagConstraints.RELATIVE;
      c.insets = new Insets(10, 10, 5, 0);
      gb.setConstraints(usernameLabel, c);
      contentPane.add(usernameLabel);

      // place the text field for the username 
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(10, 0, 5, 10);
      gb.setConstraints(usernameField, c);
      contentPane.add(usernameField);

      // place password label
      c.gridwidth = GridBagConstraints.RELATIVE;
      c.insets = new Insets(0, 10, 10, 0);
      gb.setConstraints(passwordLabel, c);
      contentPane.add(passwordLabel);

      // place the password field 
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0, 0, 10, 10);
      gb.setConstraints(passwordField, c);
      contentPane.add(passwordField);

      // place the login button
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(5, 10, 10, 10);
      c.anchor = GridBagConstraints.CENTER;
      gb.setConstraints(loginButton, c);
      contentPane.add(loginButton);

      // register password field and OK button with action event handler
      passwordField.addActionListener(this);
      loginButton.addActionListener(this);

      // anonymous inner class for closing the window
      mainFrame.addWindowListener(new WindowAdapter() 
      {
	public void windowClosing(WindowEvent e) 
	{ 
	  System.exit(0); 
	}
      });

      // size the window to obtain a best fit for the components
      mainFrame.pack();

      // center the frame
      Dimension d = mainFrame.getToolkit().getScreenSize();
      Rectangle r = mainFrame.getBounds();
      mainFrame.setLocation( (d.width - r.width)/2, (d.height - r.height)/2 );

      // make the window visible
      mainFrame.setVisible(true);

      // place the cursor in the text field for the username
      usernameField.requestFocus();

      try 
      {
	// Load the Oracle JDBC driver
	DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
      }
      catch (SQLException ex)
      {
	System.out.println("Message: " + ex.getMessage());
	System.exit(-1);
      }
    }


    /*
     * connects to Oracle database named ug using user supplied username and password
     */ 
    private boolean connect(String username, String password)
    {
      String connectURL = "jdbc:oracle:thin:@dbhost.ugrad.cs.ubc.ca:1522:ug"; 

      try 
      {
	con = DriverManager.getConnection(connectURL,username,password);

	System.out.println("\nConnected to Oracle!");
	return true;
      }
      catch (SQLException ex)
      {
	System.out.println("Message: " + ex.getMessage());
	return false;
      }
    }


    /*
     * event handler for login window
     */ 
    public void actionPerformed(ActionEvent e) 
    {
	if ( connect(usernameField.getText(), String.valueOf(passwordField.getPassword())) )
	{
	  // if the username and password are valid, 
	  // remove the login window and display a text menu 
	  mainFrame.dispose();
          showMenu();     
	}
	else
	{
	  loginAttempts++;
	  
	  if (loginAttempts >= 3)
	  {
	      mainFrame.dispose();
	      System.exit(-1);
	  }
	  else
	  {
	      // clear the password
	      passwordField.setText("");
	  }
	}             
                    
    }


    /*
     * displays simple text interface
     */ 
    private void showMenu()
    {
	int choice;
	boolean quit;

	quit = false;
	
	try 
	{
	    // disable auto commit mode
	    con.setAutoCommit(false);

	    while (!quit)
	    {
		System.out.print("\n\nPlease choose one of the following: \n");
		System.out.print("1.  Insert item\n");
		System.out.print("2.  Delete item\n");
		System.out.print("3.  Textbooks where stock is less than 10 and amount sold last week is greater than 50\n");
		System.out.print("4.  Top 3 Items Sold Last Week\n");
		System.out.print("5.  Quit\n>> ");

		choice = Integer.parseInt(in.readLine());
		
		System.out.println(" ");

		switch(choice)
		{
		   case 1:  insertItem(); break;
		   case 2:  deleteItem(); break;
		   case 3:  textBook(); break;
		   case 4:  topItems(); break;
		   case 5:  quit = true;
		}
	    }

	    con.close();
            in.close();
	    System.out.println("\nGood Bye!\n\n");
	    System.exit(0);
	}
	catch (IOException e)
	{
	    System.out.println("IOException!");

	    try
	    {
		con.close();
		System.exit(-1);
	    }
	    catch (SQLException ex)
	    {
		 System.out.println("Message: " + ex.getMessage());
	    }
	}
	catch (SQLException ex)
	{
	    System.out.println("Message: " + ex.getMessage());
	}
    }


    /*
     * inserts a item
     */ 
    private void insertItem()
    {
	String             upc;
	float              SellingPrice;
	int                stock;
	String             taxable;
	int				   isBook;
	String 			   title;
	String			   publisher;
	String			   flag_text;
	PreparedStatement  ps;
	PreparedStatement  ps2;
	  
	try
	{
		
	  System.out.print("\n0 for Book");
	  System.out.print("\n1 for Non-Book");
	  isBook = Integer.parseInt(in.readLine());
	  ps = con.prepareStatement("INSERT INTO item VALUES (?,?,?,?)");		  
	  
	  
	  
	  
	  
	  System.out.print("\nItem UPC: ");
	  upc = in.readLine();
	  ps.setString(1, upc);

	  System.out.print("\nItem Selling Prce: ");
	  SellingPrice = Float.parseFloat(in.readLine());
	  ps.setFloat(2, SellingPrice);

	  System.out.print("\nItem Stock: ");
	  stock = Integer.parseInt(in.readLine());
	  ps.setInt(3, stock);
	  
	 
	  System.out.print("\nIs Item Taxable? ");
	  taxable = in.readLine();
	  ps.setString(4, taxable);

	  

	  
	  
	  if (isBook == 0) {
		  ps2 = con.prepareStatement("INSERT INTO book VALUES (?,?,?,?)");
		  
		  ps2.setString(1, upc);
		  
		  System.out.print("\nBook Title: ");
		  title = in.readLine();
		  ps2.setString(2, title);
		  
		  System.out.print("\nBook Publisher: ");
		  publisher = in.readLine();
		  ps2.setString(3, publisher);
		  
		  System.out.print("\nIs Book a Text-Book");
		  flag_text = in.readLine();
		  ps2.setString(4, flag_text);
		  
		  ps.executeUpdate();
		  ps2.executeUpdate();
			
		  // commit work 
		  con.commit();
			
		  ps.close();
		  ps2.close();
		  
	  }
	  else {
		  ps.executeUpdate();
		  // commit work
		  con.commit();
		  ps.close();
	  }
	  
	
	

	}
	catch (IOException e)
	{
	    System.out.println("IOException!");
	}
	catch (SQLException ex)
	{
	    System.out.println("Message: " + ex.getMessage());
	    try 
	    {
		// undo the insert
		con.rollback();	
	    }
	    catch (SQLException ex2)
	    {
		System.out.println("Message: " + ex2.getMessage());
		System.exit(-1);
	    }
	}
    }


    /*
     * deletes a item
     */ 
    private void deleteItem()
    {
	String             upc;
	PreparedStatement  ps;
	  
	try
	{

	  ps = con.prepareStatement("SELECT * FROM item WHERE upc = ? AND stock = 0");
	  System.out.print("\nItem Upc: ");
	  upc = in.readLine();
	  ps.setString(1, upc);

	  int rowCount = ps.executeUpdate();

	  if (rowCount == 0)
	  {
	      System.out.println("\nItem " + upc + " does not exist or stock is not 0!");
	  }
	  
	  else {
		  
		  ps = con.prepareStatement("DELETE FROM itemPurchase WHERE upc = ?");
		  ps.setString(1, upc);
		  ps.executeUpdate();
		  
		  ps = con.prepareStatement("DELETE FROM book WHERE upc = ?");
		  ps.setString(1, upc);
		  ps.executeUpdate();
		  
		  ps = con.prepareStatement("DELETE FROM item WHERE upc = ? AND stock = 0");
		  ps.setString(1, upc);
		  ps.executeUpdate();
	  }

	  con.commit();

	  ps.close();
	}
	catch (IOException e)
	{
	    System.out.println("IOException!");
	}
	catch (SQLException ex)
	{
	    System.out.println("Message: " + ex.getMessage());

            try 
	    {
		con.rollback();	
	    }
	    catch (SQLException ex2)
	    {
		System.out.println("Message: " + ex2.getMessage());
		System.exit(-1);
	    }
	}
    }
    

    /*
     * Finds textbooks according to Q3
     */ 
    private void textBook()
    {
    	String upc;
    	Statement  stmt;
    	ResultSet  rs;
    	   
    	try
    	{
    	  stmt = con.createStatement();

    	  rs = stmt.executeQuery("SELECT * FROM book b, item i WHERE b.upc = i.upc AND i.stock < 10 AND b.flag_text = 'y' AND 50 < (SELECT SUM(ip.quantity) FROM itemPurchase ip, (SELECT p.t_id AS pid FROM purchase p WHERE p.purchaseDate >= '25-OCT-15' AND p.purchaseDate <= '31-OCT-15') WHERE pid = ip.t_id AND ip.upc = b.upc)");

    	  System.out.println("UPCs of Textbooks with stock less than 10 which have sold more than 50 copies in the last week:");
    	  while(rs.next())
    	  {
    	      // for display purposes get everything from Oracle 
    	      // as a string

    	      // simplified output formatting; truncation may occur

    	      upc = rs.getString("upc");
    	      System.out.printf("%-10.10s", upc);
    	      
    	      
    	      System.out.print("\n");
        
    	  }
     
    	  // close the statement; 
    	  // the ResultSet will also be closed
    	  stmt.close();
    	}
    	catch (SQLException ex)
    	{
    	    System.out.println("Message: " + ex.getMessage());
    	}	
        }
        
     
    
    /*
     * find top 3 items according to Q4
     */ 
    private void topItems()
    {
    String upc;
	Statement  stmt;
	ResultSet  rs;
	   
	try
	{
	  stmt = con.createStatement();

	  rs = stmt.executeQuery("SELECT * FROM (SELECT i.upc, itemQuantity, itemQuantity*i.sellingPrice FROM item i, (SELECT ip.upc AS purchaseUpc, SUM(ip.quantity) AS itemQuantity FROM purchase p, itemPurchase ip WHERE p.purchaseDate >= '25-OCT-15' AND p.purchaseDate <= '31-OCT-15' AND p.t_id = ip.t_id GROUP BY ip.upc) WHERE purchaseUpc = i.upc ORDER BY itemQuantity*i.sellingPrice desc) WHERE rownum <= 3");

	  System.out.println("UPCs of the top 3 total sales amount items in the last week:");
	  while(rs.next())
	  {
	      // for display purposes get everything from Oracle 
	      // as a string

	      // simplified output formatting; truncation may occur

	      upc = rs.getString("upc");
	      System.out.printf("%-10.10s", upc);
	      
	      
	      System.out.print("\n");
    
	  }
 
	  // close the statement; 
	  // the ResultSet will also be closed
	  stmt.close();
	}
	catch (SQLException ex)
	{
	    System.out.println("Message: " + ex.getMessage());
	}	
    }
    
 
    public static void main(String args[])
    {
      itemtable i = new itemtable();
    }
}
