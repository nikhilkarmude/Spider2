import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@SuppressWarnings("unused")
public class USF {
	static ArrayList<Heading> headT = new ArrayList<Heading>();
	static Connection conn;
	static String DBUrl, userName, Password, UniversityURL, BaseURL;
	static {
		ReadConfigFile();
	}

	static void ConnectionSetup() {
		try {
			String un = userName;// "expertnetWeb";
			String password = Password;// "Kaliban01.";
			String url = DBUrl;// "jdbc:jtds:sqlserver://cimes3.its.fsu.edu/ExpertNet2_dev;instance=SQLEXPRESS";
			Class.forName("net.sourceforge.jtds.jdbc.Driver");
			conn = DriverManager.getConnection(url, un, password);
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}

	static void ReadConfigFile() {
		Properties prop = new Properties();
		InputStream input = null;
		try {

			input = new FileInputStream(new File("./config.properties"));
			// load a properties file
			prop.load(input);

			// get the property value and print it out
			DBUrl = prop.getProperty("DBurl");
			userName = prop.getProperty("userName");
			Password = prop.getProperty("password");
			UniversityURL = prop.getProperty("universityURLUSF");
			BaseURL = prop.getProperty("baseURLUSF");

		} catch (Exception ex) {

			System.out.println("Config file not found....");
			PrintWriter writer;
			try {
				File log = new File("Error.txt");

				if (log.exists() == false) {
					System.out.println("We had to make a new file.");
					log.createNewFile();
				}
				SimpleDateFormat dt = new SimpleDateFormat(
						"yyyy-mm-dd hh:mm:ss");
				writer = new PrintWriter(new BufferedWriter(new FileWriter(log,true)));
				writer.append("\nInsert Failed:" + dt.format(new Date()));
				writer.append("\nDetails:");
				writer.append("\n\tCrwaler USF failed:");
				writer.append("\n\tReason config File not found!!");
				writer.append("\n######################################\n\n");
				writer.close();
				System.exit(0);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}

			// UniversityURL = "http://www.research.usf.edu/dpl/tech.asp";
			// BaseURL = "http://www.research.usf.edu/";

		} finally {
			System.out.println("***********************");
			System.out.println("Database URL: " + DBUrl);
			System.out.println("\t Username: " + userName);
			System.out.println("\t Password: " + Password);
			System.out.println("University URL: " + UniversityURL);
			System.out.println("Base URL: " + BaseURL);

			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void createList() {
		try {
			Document doc = Jsoup.connect(UniversityURL).get();
			Elements Header = doc.select(".menuheader.expandable");
			// System.out.println("Number of Header" + headT.length);
			for (int i = 0; i < Header.size(); i++) {
				headT.add(new Heading());
				headT.get(i).Heading = Header.get(i).text();
				System.out.println("Heading " + headT.get(i).Heading);
				Elements cat1 = doc.select(".categoryitems");
				Elements cat = cat1.get(i).select(".subexpandable");// ("li");
				// if no subheading present
				if (cat.size() == 0) {
					// headT[i].subH = new SubHeading[1];
					// SubHeading shed = new SubHeading();
					headT.get(i).subH.add(new SubHeading());
					Elements subcategoryitems = cat1.get(i).select("li");
					for (int k = 0; k < subcategoryitems.size(); k++) {
						int sizeSubitem = headT.get(i).subH.get(0).ref.size();
						// headT.get(i).subH.get(0).ref.add(new Materials());
						headT.get(i).subH.get(0).ref.add(new Materials());
						headT.get(i).subH.get(0).ref.get(sizeSubitem).heading = headT
								.get(i).Heading;
						headT.get(i).subH.get(0).ref.get(sizeSubitem).subHeading = headT
								.get(i).subH.get(0).subHeading;
						headT.get(i).subH.get(0).ref.get(sizeSubitem).url = BaseURL
								+ subcategoryitems.get(k).select("a")
										.attr("href");
						String temp = subcategoryitems.get(k).tagName("a")
								.text();
						headT.get(i).subH.get(0).ref.get(sizeSubitem).ID = temp
								.substring(0, temp.indexOf(' ')).trim();
						headT.get(i).subH.get(0).ref.get(sizeSubitem).Title = temp
								.substring(temp.indexOf(' ') + 2).trim();
						headT.get(i).subH.get(0).ref.get(sizeSubitem).keyword = headT
								.get(i).Heading
								+ ","
								+ headT.get(i).subH.get(0).subHeading;

						boolean foundDuplicate = findMatchID(
								headT,
								headT.get(i).subH.get(0).ref.get(sizeSubitem).ID,
								headT.get(i).Heading,
								headT.get(i).subH.get(0).subHeading,
								headT.get(i).subH.get(0).ref.get(sizeSubitem).Title,
								headT.get(i).subH.get(0).ref.get(sizeSubitem).keyword);
						if (foundDuplicate) {
							headT.get(i).subH.get(0).ref.remove(sizeSubitem);
						} else {

							String text = GetDataFromPDF(headT.get(i).subH
									.get(0).ref.get(sizeSubitem).url);
							text = DeleteUnWanted(text,
									headT.get(i).subH.get(0).ref
											.get(sizeSubitem).Title);

							text = massageDescription(text,
									headT.get(i).subH.get(0).ref
											.get(sizeSubitem).Title);
							headT.get(i).subH.get(0).ref.get(sizeSubitem).Description = text;

						}
					}
				} else {
					// if sub heading present
					//
					for (int j = 0; j < cat.size(); j++) {
						int subHEadingIndex = headT.get(i).subH.size();
						headT.get(i).subH.add(new SubHeading());
						headT.get(i).subH.get(subHEadingIndex).subHeading = cat
								.get(j).text();
						// if(headT[i].subH[j].subHeading.equals(""))
						// continue;
						Elements subcategoryitems = cat1.get(i)
								.select(".subcategoryitems").get(j)
								.select("li");
						for (int k = 0; k < subcategoryitems.size(); k++) {
							int subcategoryitemsIndex = headT.get(i).subH
									.get(subHEadingIndex).ref.size();
							headT.get(i).subH.get(subHEadingIndex).ref
									.add(new Materials());
							headT.get(i).subH.get(subHEadingIndex).ref
									.get(subcategoryitemsIndex).url = BaseURL
									+ subcategoryitems.get(k).select("a")
											.attr("href");
							// String
							// temp[]=subcategoryitems.get(k).tagName("a").text().substring(0,
							// endIndex);
							headT.get(i).subH.get(subHEadingIndex).ref
									.get(subcategoryitemsIndex).heading = headT
									.get(i).Heading.trim();
							headT.get(i).subH.get(subHEadingIndex).ref
									.get(subcategoryitemsIndex).subHeading = headT
									.get(i).subH.get(j).subHeading.trim();
							String temp = subcategoryitems.get(k).tagName("a")
									.text();
							headT.get(i).subH.get(subHEadingIndex).ref
									.get(subcategoryitemsIndex).ID = temp
									.substring(0, temp.indexOf(' ')).trim();

							headT.get(i).subH.get(j).ref
									.get(subcategoryitemsIndex).Title = temp
									.substring(temp.indexOf(' ') + 2).trim();
							String title = headT.get(i).subH.get(j).ref
									.get(subcategoryitemsIndex).Title;
							headT.get(i).subH.get(subHEadingIndex).ref
									.get(subcategoryitemsIndex).keyword = headT
									.get(i).Heading.trim()
									+ ", "
									+ headT.get(i).subH.get(j).subHeading
											.trim();
							boolean foundDuplicate = findMatchID(
									headT,
									headT.get(i).subH.get(subHEadingIndex).ref
											.get(subcategoryitemsIndex).ID,
									headT.get(i).Heading,
									headT.get(i).subH.get(subHEadingIndex).subHeading,
									headT.get(i).subH.get(subHEadingIndex).ref
											.get(subcategoryitemsIndex).Title,
									headT.get(i).subH.get(subHEadingIndex).ref
											.get(subcategoryitemsIndex).keyword);
							if (foundDuplicate) {
								headT.get(i).subH.get(subHEadingIndex).ref
										.remove(subcategoryitemsIndex);
							} else {

								String text = GetDataFromPDF(headT.get(i).subH
										.get(subHEadingIndex).ref
										.get(subcategoryitemsIndex).url);
								// Massaging

								text = DeleteUnWanted(text, title);

								text = massageDescription(text, title);
								if (headT.get(i).subH.get(subHEadingIndex).ref
										.get(subcategoryitemsIndex).ID
										.equals("08A013")) {
									text = "<div align='justify'>R"
											+ text.substring(
													text.lastIndexOf("esearchers at"),
													text.length());

								}
								headT.get(i).subH.get(subHEadingIndex).ref
										.get(subcategoryitemsIndex).Description = text;

							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static String massageDescription(String Text, String Title) {

		Text = Text.replaceAll("\r\n", "<br />");
		Text = Text.replaceAll("\n", "<br />");
		Text = Text.replaceAll("\t", " ");
		// Text = Text.replaceAll("\r", "<br />");

		Text = ReplaceAllTo(Text, "Technological Advantages");
		Text = ReplaceAllTo(Text, "Technological Advantage");

		Text = ReplaceAllTo(Text, "Description");
		Text = ReplaceAllTo(Text, "How This New Technology Works");
		Text = ReplaceAllTo(Text, "TECHNOLOGY HIGHLIGHTS");
		Text = ReplaceAllTo(Text, "Advantages");
		Text = ReplaceAllTo(Text, "Advantage");
		Text = reFormat(Text);
		// Text=Text.replaceFirst("R esearchers" , "Researchers");
		// Text=Text.replaceFirst("E ach", "Each");
		// Text=Text.replaceFirst("W hen", "When");
		Text = Text.trim();
		if (Text.indexOf(" ") == 1) {

			if (Character.isUpperCase(Text.charAt(0))
					&& (Character.isLowerCase(Text.substring(1, Text.length())
							.trim().charAt(0)))) {
				if (Text.charAt(0) != 'A')
					Text = Text.replaceFirst("\\s+", "");

			}
		}
		Text = Text.replaceAll("<br />(\\s*<br />)*", "<br />");
		Text = Text.replaceAll("<br />", "<br /><br />");
		Text = "<div align='justify'><center><br /><br /></center>" + Text + "</div>";

		return Text;

	}

	static String DeleteUnWanted(String str, String title) {
		if (title.equalsIgnoreCase("New ras/p27 Mouse Tumorigenesis Model"))
			System.out.print("");
		str = ReplaceWithNullEntireLine(str, "Copyright ©");
		str = ReplaceWithNullEntireLine(str, " Division of Patents");

		str = ReplaceWithNullEntireLine(str, "NEW patents & licensing");
		str = ReplaceWithNullEntireLine(str, "O f f i c e o f R e s e a r c h");
		str = ReplaceWithNullEntireLine(str, "Florida 33612");
		str = ReplaceWithNullEntireLine(str, " Tampa, Florida");
		str = ReplaceWithNullEntireLine(str, "Orlando, FL 32826");
		str = ReplaceWithNullEntireLine(str, "http://www.research.usf.edu");
		try {
			str = str.replaceAll("^\\s*" + title + "\\s*(\r?\n|\r)", "\n");
		} catch (Exception e) {

		}

		// str = ReplaceWithNullEntireLine(str, "(?i)contact");
		str = ReplaceWithNullEntireLine(str, "(?i)TECH ID");
		str = ReplaceWithNullEntireLine(str, "(?i)Tech ID");
		str = ReplaceWithNullEntireLine(str, "(?i)Tech\\s*ID");
		str = ReplaceWithNullEntireLine(str, "USF Tech");
		str = ReplaceWithNullEntireLine(str, "R e s e a r c h");
		str = ReplaceWithNullEntireLine(str, "NEW patents & licensing");
		str = ReplaceWithNullEntireLine(str, "patents@research.usf.edu");
		str = ReplaceWithNullEntireLine(str, "(?i)Figure");
		str = ReplaceWithNullEntireLine(str, "East Fowler Avenue");
		str = ReplaceWithNullEntireLine(str, "Office of Research");
		str = ReplaceWithNullEntireLine(str, "LICENSING OPPORTUNITY");
		str = ReplaceWithNullEntireLine(str, "seeks partners to license");
		str = ReplaceWithNullEntireLine(str, "Research Office");
		str = ReplaceWithNullEntireLine(str, "Division of Patents & Licensing");
		str = ReplaceWithNullEntireLine(str, "(?i)FIG:");
		str = ReplaceWithNullEntireLine(str, "(?i)NEW patents");
		str = ReplaceWithNullEntireLine(str, "Oncogene\\s*(2000)");
		str = ReplaceWithNullEntireLine(str, "5338-5347");
		str = ReplaceWithNullEntireLine(str, " 813\\.974\\.8490");
		str = ReplaceWithNullEntireLine(str, "(fax)");
		str = ReplaceWithNullEntireLine(str, "(office)");
		// str = str.replaceAll("\\d+(\r?\n|\r)?", "");
		str = str.replaceAll("•", "&nbsp&nbsp•&nbsp&nbsp");
		str = str.replaceAll("", "&nbsp&nbsp•&nbsp&nbsp");
		str = str.replaceAll("■", "&nbsp&nbsp • &nbsp&nbsp");
		str = str.replaceAll("", "&nbsp&nbsp • &nbsp&nbsp");
		str = str.replaceAll("\\s*(?i)contact\\s*(\r?\n|\r)", "\n");

		return str;

	}

	static String reFormat(String str) {
		String temp1 = "";
		boolean foundDot = false;
		boolean blankLine = false;
		String temp[] = str.split("<br />");

		for (int i = 0; i < temp.length; i++) {
			int curri = i;
			if (temp[i].contains("<strong>")) {
				temp[i] = "<br /><br />" + temp[i] + "<br />";
			}
			if (temp[i].equals("")) {
				if (i > 1)
					if (!temp[i - 1].equalsIgnoreCase("") && !blankLine) {
						temp[i] = "<br />";
						blankLine = true;
					}
			} else {
				blankLine = false;
			}
			if (i > 1) {
				if (foundDot) {
					if (!temp[i].contains("•")) {
						if ((i) > temp.length - 2) {
							temp[i] = temp[i] + "<br />";
						} else {
							if (temp[i].trim().length() > 0) {
								if (!Character.isLowerCase(temp[i].trim()
										.charAt(0))) {
									if (!Character.isDigit(temp[i].trim()
											.charAt(0))) {
										temp[i] = "<br /><br />" + temp[i];
										foundDot = false;
									}
								}
							} else {
								temp[i] = temp[i] + "<br />";
								foundDot = false;
							}

						}
					} else {
						foundDot = false;
					}
				}
			}
			if (temp[i].contains("•")) {
				foundDot = true;
				temp[i] = "<br />" + temp[i];
			}
			// temp[i] = temp[i].replaceAll("[0-9]+?", "");
			if (curri != i)
				System.out.print("");
			temp1 += temp[i];
		}

		return temp1;
	}

	static String ReplaceAllTo(String Text, String ReplaceWord) {

		try {
			Text = Text.replaceAll(ReplaceWord + "([ ]*)(:?)([ ]*)<br />",
					"<br /><strong>" + ReplaceWord + "</strong><br />");
		} catch (Exception e) {

		}
		return Text;
	}

	static String ReplaceWithNullEntireLine(String text, String TextToFind) {
		try {
			text = text.replaceAll(".*" + TextToFind + ".*(\r?\n|\r)?", "\n");
		} catch (Exception e) {

		}
		return text;
	}

	public static String GetDataFromPDF(String url) {
		if (!url.contains(".pdf"))
			return "";
		System.out.println(url);
		PDDocument pdf = null;
		PDFTextStripper stripper = null;
		try {
			stripper = new PDFTextStripper();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			// e1.printStackTrace();
		}
		String text = "";
		while (true) {
			boolean exc = false;
			try {
				pdf = PDDocument.load(new URL(url.replace("https", "http")));

				// pdf.save("back.pdf");
				text = stripper.getText(pdf);
				// text = text.replaceAll("[^\\x0A\\x0D\\x20-\\x7E]", "");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			} finally {
				try {
					pdf.close();
				} catch (Exception e) {

				}
			}
			if (text.equals("") || text.equals(null)) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
				}
			} else {
				break;
			}
		}
		return text;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Creating List");
		createList();
		System.out.println("Send Data To DB");
		DBHandle();
		System.out.println("Output");
		Diplay();
	}

	static boolean findMatchID(ArrayList<Heading> headT, String ID,
			String HeadingT, String SubHeadingT, String Title, String keyword) {
		boolean found = false;
		for (int h = 0; h < headT.size(); h++) {
			for (int i = 0; i < headT.get(h).subH.size(); i++) {
				for (int j = 0; j < headT.get(h).subH.get(i).ref.size(); j++) {
					if (headT.get(h).subH.get(i).ref.get(j).ID.equals(ID)) {
						// System.out.println("ID MAtch Found \n at "+headT.get(h).subH.get(i).subHeading+" Which is present in "+SubHeadingT);
						if (!checkForDuplicateKeywords(
								headT.get(h).subH.get(i).ref.get(j).keyword,
								HeadingT)) {
							// System.out.println("Heading mismatch from "+headT.get(h).Heading+"At"+HeadingT);
							// headT.get(h).subH.get(i).ref.get(j).heading +=
							// ","
							// + HeadingT;
							headT.get(h).subH.get(i).ref.get(j).keyword += ", "
									+ HeadingT.trim();
							found = true;
						}
						if (SubHeadingT == null)
							SubHeadingT = "";
						if (headT.get(h).subH.get(i).ref.get(j).subHeading == null)
							headT.get(h).subH.get(i).ref.get(j).subHeading = "";
						if (!checkForDuplicateKeywords(
								headT.get(h).subH.get(i).ref.get(j).keyword,
								SubHeadingT)) {
							// System.out.println("SubHeading mismatch from "+headT.get(h).subH.get(i).subHeading+"At"+SubHeadingT);
							if (checkForDuplicateKeywords(
									headT.get(h).subH.get(i).ref.get(j).keyword,
									HeadingT)) {
								String[] split = headT.get(h).subH.get(i).ref
										.get(j).keyword.split(",");
								for (int k = 0; k < split.length; k++) {
									if (split[k].trim().equals(HeadingT)) {
										split[k] += ", " + SubHeadingT.trim();
										break;
									}
								}
								String temp = split[0];
								for (int k = 1; k < split.length; k++) {
									temp += ", " + split[k].trim();
								}
								headT.get(h).subH.get(i).ref.get(j).keyword = temp;
							}
							// headT.get(h).subH.get(i).ref.get(j).keyword +=
							// keyword;
							found = true;
						}
						if (!headT.get(h).subH.get(i).ref.get(j).Title
								.contains(Title)) {
							// System.out.println("Title Mismatch");
							// headT.get(h).subH.get(i).ref.get(j).Title +=
							// ", "+ Title;
							found = true;
						}
						if (!headT.get(h).subH.get(i).ref.get(j).keyword
								.equals(keyword)) {
							return true;
						}
						if (found == true) {
							return found;
						}
					}
				}
			}
		}
		return found;
	}

	static boolean checkForDuplicateKeywords(String keywords, String match) {
		String[] split = keywords.split(",");
		for (int k = 0; k < split.length; k++) {
			if (split[k].trim().equals(match.trim())) {
				return true;
			}
		}
		return false;
	}

	static void Diplay() {
		int cnt = 1;
		System.out.println("Output");
		System.out
				.println("<tr><th>Number</th><th>Heading</th><th>subheading</th><th>Keyword</th><th>UID</th><th> Title</th><th> URL</th><th> Description</th></tr>");
		for (int j = 0; j < headT.size(); j++) {
			// System.out.println("Heading:\t" + headT.get(j).Heading);
			for (int j2 = 0; j2 < headT.get(j).subH.size(); j2++) {
				for (int k = 0; k < headT.get(j).subH.get(j2).ref.size(); k++) {

					System.out.print("<tr>");
					System.out.print("<td>" + (cnt++) + "</td><td>"
							+ headT.get(j).subH.get(j2).ref.get(k).heading
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).subHeading
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).keyword
							+ "</td>");
					System.out
							.print("<td>"
									+ headT.get(j).subH.get(j2).ref.get(k).ID
									+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).Title
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).url
							+ "</td>");
					System.out.print("<td>"
							+ headT.get(j).subH.get(j2).ref.get(k).Description
							+ "</td>");
					System.out.print("</tr>");
					System.out.println();
				}
			}
		}
	}

	static void checkDuplicate(ArrayList<Materials> m,
			ArrayList<SubHeading> shed, ArrayList<Heading> head, String UID) {
	}

	static void DBHandle() {
		ConnectionSetup();
		for (int j = 0; j < headT.size(); j++) {
			// System.out.println("Heading:\t" + headT.get(j).Heading);
			for (int j2 = 0; j2 < headT.get(j).subH.size(); j2++) {
				for (int k = 0; k < headT.get(j).subH.get(j2).ref.size(); k++) {
					try {
						String sql = "INSERT INTO dbo.propertyHolding (infoURL, contactEmail, fkUniversityID, Title,Keywords,universituid,MarketingPDF,Description) "
								+ "VALUES (?, ?, ?, ?,?,?,?,?)";
						PreparedStatement statement = conn
								.prepareStatement(sql);
						statement.setString(1,
								headT.get(j).subH.get(j2).ref.get(k).url);
						statement.setString(2, "patents@research.usf.edu");
						statement.setInt(3, 9);
						statement.setString(4,
								headT.get(j).subH.get(j2).ref.get(k).Title);
						String keywords = headT.get(j).subH.get(j2).ref.get(k).keyword;
						statement.setString(5, keywords);
						statement.setString(6,
								headT.get(j).subH.get(j2).ref.get(k).ID);
						statement.setString(7,
								headT.get(j).subH.get(j2).ref.get(k).url);
						statement
								.setString(
										8,
										headT.get(j).subH.get(j2).ref.get(k).Description);
						int rowsInserted = statement.executeUpdate();
						if (rowsInserted > 0) {
							System.out
									.println("A new record was inserted successfully!");
						} else {
							System.out
									.println("A new record was inserted Failed!");
						}
					} catch (Exception e) {
						System.out.println("A new record was inserted Failed!");
					}
				}
			}
		}
	}
}

class Heading {
	String Heading;
	ArrayList<SubHeading> subH = new ArrayList<SubHeading>();
}

class SubHeading {
	String subHeading;
	ArrayList<Materials> ref = new ArrayList<Materials>();
}

class Materials {
	String keyword;
	String heading;
	String subHeading;
	String url;
	String Title;
	String ID;
	String Description;
}
