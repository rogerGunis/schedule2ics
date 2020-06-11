$(document).ready(function()   { 
                
                var nr = 0;
                $('[class*="MittagessenRound-"]').each(function(i,ele) {
                   nr = ele.className.match(/MittagessenRound-(.*)/)[1];
                });

                for ( var i = 1, l = nr; i <= l; i++ ) {
                   var myClass = ".MittagessenRound-"+i;
                   var addMyClass = "round"+(i % 2+1);

                   $(this).find(myClass).parent().parent().addClass(addMyClass).length;
                }
                
                var nr = 0;
                $('[class*="PutzenRound-"]').each(function(i,ele) {
                   nr = ele.className.match(/PutzenRound-(.*)/)[1];
                });

                for ( var i = 1, l = nr; i <= l; i++ ) {
                   var myClass = ".PutzenRound-"+i;
                   var addMyClass = "lround"+(i % 2+1);

                   $(this).find(myClass).parent().parent().addClass(addMyClass).length;
                }

                var nr = 0;
                $('[class*="Fr--hst--ckRound-"]').each(function(i,ele) {
                   nr = ele.className.match(/Fr--hst--ckRound-(.*)/)[1];
                });

                for ( var i = 1, l = nr; i <= l; i++ ) {
                   var myClass = ".Fr--hst--ckRound-"+i;
                   var addMyClass = "rround"+(i % 2+1);

                   $(this).find(myClass).parent().parent().addClass(addMyClass).length;
                }
});
