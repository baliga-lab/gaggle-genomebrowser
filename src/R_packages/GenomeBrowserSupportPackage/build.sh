rm *.gz
rm *.zip
R --no-init-file CMD build GenomeBrowserSupport
sudo R --no-init-file CMD INSTALL *.gz
