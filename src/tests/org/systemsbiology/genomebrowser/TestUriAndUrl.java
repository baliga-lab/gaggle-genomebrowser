package org.systemsbiology.genomebrowser;

import java.net.URI;
import java.net.URL;

public class TestUriAndUrl {

	public static void main(String[] args) throws Exception {
		URI uri;
		URL url = new URL("file:/docs/index.php");
		System.out.println("protocol = " + url.getProtocol());
		System.out.println("auth     = " + url.getAuthority());
		System.out.println("host     = " + url.getHost());
		System.out.println("path     = " + url.getPath());
		System.out.println("file     = " + url.getFile());
		System.out.println("ref      = " + url.getRef());
		System.out.println();

		url = TestUriAndUrl.class.getResource("/icons/halo1.png");
		System.out.println("url      = " + url);
		System.out.println("protocol = " + url.getProtocol());
		System.out.println("auth     = " + url.getAuthority());
		System.out.println("host     = " + url.getHost());
		System.out.println("path     = " + url.getPath());
		System.out.println("file     = " + url.getFile());
		System.out.println("ref      = " + url.getRef());
		System.out.println();

		url = TestUriAndUrl.class.getResource("/HaloTilingArrayReferenceConditions/chromosome/halo1.png");
		System.out.println("url      = " + url);
		System.out.println("protocol = " + url.getProtocol());
		System.out.println("auth     = " + url.getAuthority());
		System.out.println("host     = " + url.getHost());
		System.out.println("path     = " + url.getPath());
		System.out.println("file     = " + url.getFile());
		System.out.println("ref      = " + url.getRef());
		System.out.println();

		uri = new URI("classpath:/HaloTilingArrayReferenceConditions/chromosome/halo1.png");
		System.out.println("uri      = " + uri);
		System.out.println("auth     = " + uri.getAuthority());
		System.out.println("host     = " + uri.getHost());
		System.out.println("path     = " + uri.getPath());
		System.out.println("fragment     = " + uri.getFragment());
		System.out.println();

		uri = new URI("jar:file:/Users/cbare/Documents/work/eclipse-workspace-isb/GenomeBrowser/data/HaloTilingArrayReferenceConditions.zip!/HaloTilingArrayReferenceConditions/halo.dataset");
		System.out.println("uri      = " + uri);
		System.out.println("auth     = " + uri.getAuthority());
		System.out.println("host     = " + uri.getHost());
		System.out.println("path     = " + uri.getPath());
		System.out.println("fragment     = " + uri.getFragment());
		System.out.println();

		System.out.println();
	}
}
